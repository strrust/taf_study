def projectDir = 'C:\\Users\\Rustam_Saitov\\Desktop\\pet\\taf_study'

// Set up the Jenkins pipeline
pipeline {
    agent any

    environment {
        USER_SCRIPT_PATH = "C:\\Users\\Rustam_Saitov\\AppData\\Local\\Programs\\Python\\Python310\\Scripts"
    }

    stages {
        stage('Build') {
            steps {
                // Go to the project directory
                dir(projectDir) {
                    // Run the build command
                    bat 'mvn clean'
                }
            }
        }
        stage('Test') {
            steps {
                // Go to the project directory
                dir(projectDir) {
                    // Run the test command
                    bat 'mvn test'
                }
            }
        }
    }
    //Report workflow
    post {
        always {
            script {
                dir(projectDir) {
                    bat """
                    "${env.USER_SCRIPT_PATH}\\junitparser.exe" merge \
                    --glob "target/surefire-reports/junitreports/TEST-*" "target/surefire-reports/customReport.xml"
                    """
                    bat """
                    "${env.USER_SCRIPT_PATH}\\trcli.exe" -y \
                    --config trcli_config.yml \
                    parse_junit \
                    --title "Automated Regression Test Run ${BUILD_TIMESTAMP}" \
                    --run-description "Regression Time: ${BUILD_TIMESTAMP}" \
                    -f "target/surefire-reports/customReport.xml"
                    """
                }
            }
        }
    }
}