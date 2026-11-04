pipeline {
    agent any

    tools {
        maven 'Maven3'            // nom défini dans Manage Jenkins > Tools
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '3'))
    }

    triggers {
        pollSCM('H/2 * * * *')    // interroge GitHub toutes les ~2 minutes
    }

    environment {
        TAG          = "${env.BUILD_NUMBER}"
        MODULES      = 'discovery-server config-server api-gateway employee-service leave-service payroll-service notification-service'
        COMPOSE_FILE = 'docker-compose.deploy.yml'
    }

    stages {

        // CI -------------------------------------------------------------
        stage('Build & Test') {
            steps {
                sh 'mvn -B -ntp clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: '*/target/*.jar', fingerprint: true
            }
        }

        // Livraison : une image Docker par module -------------------------
        stage('Docker images') {
            steps {
                sh '''
                    set -e
                    for m in $MODULES; do
                        echo "=== Image hr/$m:$TAG ==="
                        docker build --build-arg JAR_FILE=$m/target/*.jar \
                                     -t hr/$m:$TAG -t hr/$m:latest .
                    done
                '''
            }
        }

        // CD --------------------------------------------------------------
        stage('Deploy') {
            when {
                expression { env.GIT_BRANCH ==~ /(origin\/)?main/ }
            }
            steps {
                sh 'docker compose up -d --build --remove-orphans'
            }
        }

        stage('Smoke test') {
            when {
                expression { env.GIT_BRANCH ==~ /(origin\/)?main/ }
            }
            steps {
                sh '''
                    for i in $(seq 1 40); do
                        code=$(curl -s -o /dev/null -w "%{http_code}" \
                               http://host.docker.internal:8000/employee-service/api/employees || true)
                        echo "Tentative $i : HTTP $code"
                        if [ "$code" = "200" ]; then exit 0; fi
                        sleep 5
                    done
                    echo "Smoke test en échec : la stack ne répond pas"
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo "Build ${env.BUILD_NUMBER} OK. Application : http://localhost:8000"
        }
        failure {
            sh 'docker compose ps || true'
            sh 'docker compose logs --tail=40 || true'
        }
    }
}