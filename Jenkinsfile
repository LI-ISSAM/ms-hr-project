pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '3'))
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    environment {
        TAG          = "${env.BUILD_NUMBER}"
        MODULES      = 'discovery-server config-server api-gateway employee-service leave-service payroll-service notification-service'
        COMPOSE_FILE = 'docker-compose.deploy.yml'
    }

    stages {

        // Base PostgreSQL dédiée aux tests ------------------------------
        stage('Start test DB') {
            steps {
                sh '''
                    docker compose up -d postgres
                    until docker compose exec -T postgres pg_isready -U postgres; do
                        echo "Attente de PostgreSQL..."; sleep 2
                    done
                    for db in employee_test leave_test payroll_test; do
                        docker compose exec -T postgres psql -U postgres -c "CREATE DATABASE $db" || true
                    done
                '''
            }
        }

        // CI -------------------------------------------------------------
        stage('Build & Test') {
            steps {
                sh '''
                    set -e
                    PG=jdbc:postgresql://host.docker.internal:5433
                    NOCLOUD="-Dspring.cloud.config.enabled=false -Dspring.cloud.discovery.enabled=false -Deureka.client.enabled=false"

                    # 1) Serveurs d'infrastructure (pas de base de données)
                    mvn -B -ntp clean verify -pl discovery-server,config-server -am

                    # 2) Gateway et notification (sans base, sans config-server ni Eureka)
                    mvn -B -ntp verify -pl api-gateway,notification-service -am $NOCLOUD

                    # 3) Services métier : une base de test par service
                    for pair in employee-service:employee_test leave-service:leave_test payroll-service:payroll_test; do
                        module=${pair%%:*}
                        db=${pair##*:}
                        echo "=== Tests $module sur $db ==="
                        mvn -B -ntp verify -pl $module -am $NOCLOUD \
                            -Dspring.datasource.url=$PG/$db \
                            -Dspring.datasource.username=postgres \
                            -Dspring.datasource.password=postgres \
                            -Dspring.datasource.driver-class-name=org.postgresql.Driver \
                            -Dspring.jpa.hibernate.ddl-auto=update
                    done
                '''
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