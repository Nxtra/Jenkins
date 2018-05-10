//windows? change sh by bat
node {

    stage('checkout'){
        def gitUrl = 'https://github.com/Nxtra/solitaire-systemjs-course.git'
        echo "checking out : ${gitUrl}"
        git branch: 'jenkins2-course',
        url: gitUrl
    }


    stage('build'){
        // pull dependencies from npm
        sh 'npm install'

        // stash code & dependencies to expedite subsequent testing
        // and ensure same code & dependencies are used throughout the pipeline
        // stash is a temporary archive
        stash name: 'everything',
              excludes: 'test-results/**',
              includes: '**'
    }

    stage('Phantom test'){
        // test with PhantomJS for "fast" "generic" results
        sh 'npm run test-single-run -- --browsers PhantomJS'
    }


    stage('archive'){
        //archive app code
        archiveArtifacts 'app/**/*.* '

        // archive karma test results (karma is configured to export junit xml files)
        step([$class: 'JUnitResultArchiver',
        testResults: 'test-results/**/test-results.xml'])
    }
}

//parallel integration testing
//add dependency if you add browser
stage ('Browser Testing'){
    parallel chrome: {
        runTests("Chrome")
    }, firefox: {
        runTests("Firefox")
    }, safari: {
        runTests("Safari")
    }, opera: {
        runTests("Opera")
    }
}

stage(name: 'Deploy to staging'){

    input 'Deploy to staging?'

    node {
        // write build number to index page so we can see this update
        sh "echo '${env.BUILD_DISPLAY_NAME}' >> app/index.html"

        // deploy to a docker container mapped to port 3000
        sh 'docker-compose up -d --build'

//        notify 'Application Deployed!'
    }
}


def runTests(browser) {
    node {
        // on windows use: bat 'del /S /Q *'
        sh 'rm -rf *'

        //we might get a different workspace cause we work with multiple agents
        //so we need to unstach everything that we stached from our previous workspace to be sure we have all data
        unstash 'everything'

        // inject browser parameter
        sh "npm run test-single-run -- --browsers ${browser}"

        step([$class: 'JUnitResultArchiver',
              testResults: 'test-results/**/test-results.xml'])
    }
}

def notify(status){
    emailext (
      to: "nick*****@gmail.com",
      subject: "${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: """<p>${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>""",
    )
}