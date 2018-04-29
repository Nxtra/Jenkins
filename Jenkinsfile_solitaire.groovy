
node {

    stage('checkout'){
        def gitUrl = 'https://github.com/Nxtra/solitaire-systemjs-course.git'
        echo "checking out : ${gitUrl}"
        git branch: 'jenkins2-course',
        url: gitUrl
    }


    stage('build'){
        // pull dependencies from npm
        // on windows use: bat 'npm install'
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
        // on windows use: bat 'npm run test-single-run -- --browsers PhantomJS'
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

node('mac'){
    sh 'ls'
    sh 'rm -rf *'
    unstash 'everything'
    sh 'ls'
}

//parallel integration testing
stage ('Browser Testing'){
    parallel chrome: {
        runTests("Chrome")
    }, firefox: {
        runTests("Firefox")
    }, safari: {
        runTests("Safari")
    }
}

node {
    notify("Deploy to staging?")
}

input 'Deploy to staging?'


def runTests(browser) {
    node {
        // on windows use: bat 'del /S /Q *'
        sh 'rm -rf *'

        unstash 'everything'

        // on windows use: bat "npm run test-single-run -- --browsers ${browser}"
        // inject browser parameter
        sh "npm run test-single-run -- --browsers ${browser}"

        step([$class: 'JUnitResultArchiver',
              testResults: 'test-results/**/test-results.xml'])
    }
}

def notify(status){
    emailext (
      to: "wesmdemos@gmail.com",
      subject: "${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: """<p>${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>""",
    )
}