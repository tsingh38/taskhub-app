pipeline {
  agent any

  options {
    disableConcurrentBuilds()
  }

  environment {
    REGISTRY = "tsingh38"
    IMAGE_NAME = "taskhub"
    DOCKERHUB_CREDENTIALS = "dockerhub-tsingh38-taskhub"
    TRIVY_CACHE_DIR = "/var/lib/jenkins/.cache/trivy"
  }

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Validate Branch (develop only)') {
      steps {
        script {
          def branch = (env.BRANCH_NAME ?: env.GIT_BRANCH ?: '')
          if (!(branch == 'develop' || branch == 'origin/develop')) {
            error("taskhub-ci-dev runs only for develop. Current branch=${branch}")
          }
          echo "Branch OK: ${branch}"
        }
      }
    }

    stage('Resolve Version') {
      steps {
        dir('services/task-service') {
          script {
            def version = sh(
              script: "./gradlew properties -q | grep '^version:' | awk '{print \$2}'",
              returnStdout: true
            ).trim()

            env.APP_VERSION = version
            echo "APP_VERSION=${env.APP_VERSION}"
          }
        }
      }
    }

    stage('Compute Image Tag') {
      steps {
        script {
          env.IMAGE_TAG = "${env.APP_VERSION}-${env.BUILD_NUMBER}"
          env.IMAGE = "${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
          echo "IMAGE_TAG=${env.IMAGE_TAG}"
          echo "IMAGE=${env.IMAGE}"
        }
      }
    }

    stage('Build & Test') {
      steps {
        dir('services/task-service') {
          sh '''
            set -eu
            chmod +x gradlew
            ./gradlew clean test
          '''
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'services/task-service/build/test-results/test/*.xml'
          archiveArtifacts artifacts: 'services/task-service/build/reports/tests/test/**', allowEmptyArchive: true
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        dir('services/task-service') {
          withCredentials([usernamePassword(
            credentialsId: DOCKERHUB_CREDENTIALS,
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
          )]) {
            sh '''
              set -eu
              echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
              docker build -t "$IMAGE" .
              docker push "$IMAGE"
              echo "Built and pushed: $IMAGE"
            '''
          }
        }
      }
    }

   stage('Trivy Scan (report + gate)') {
     steps {
       withCredentials([usernamePassword(
         credentialsId: DOCKERHUB_CREDENTIALS,
         usernameVariable: 'DOCKER_USER',
         passwordVariable: 'DOCKER_PASS'
       )]) {
         sh '''#!/bin/bash
           set -euo pipefail
           mkdir -p "$TRIVY_CACHE_DIR"

           echo "=== Trivy report (always) ==="
           docker run --rm \
             -v "$TRIVY_CACHE_DIR:/root/.cache/" \
             -v "$WORKSPACE:/work" \
             aquasec/trivy:0.69.1 \
             image \
             --scanners vuln \
             --skip-version-check \
             --timeout 10m \
             --no-progress \
             --severity HIGH,CRITICAL \
             --format json \
             -o /work/trivy-report.json \
             --username "$DOCKER_USER" \
             --password "$DOCKER_PASS" \
             "$IMAGE" || true

           echo ""
           echo "=== Trivy gate (CRITICAL only) ==="
           docker run --rm \
             -v "$TRIVY_CACHE_DIR:/root/.cache/" \
             aquasec/trivy:0.69.1 \
             image \
             --scanners vuln \
             --skip-version-check \
             --timeout 10m \
             --no-progress \
             --severity CRITICAL \
             --ignore-unfixed \
             --exit-code 1 \
             --format table \
             --username "$DOCKER_USER" \
             --password "$DOCKER_PASS" \
             "$IMAGE"
         '''
       }
     }
   }
      post {
        always {
          archiveArtifacts artifacts: 'trivy-report.json', fingerprint: true, onlyIfSuccessful: false
        }
      }
    }

    stage('Trigger DEV Deploy') {
      steps {
        script {
          echo "Triggering taskhub-deploy-dev with IMAGE_TAG=${env.IMAGE_TAG}"
          build job: 'taskhub-deploy-dev',
            parameters: [
              string(name: 'IMAGE_TAG', value: env.IMAGE_TAG)
            ],
            wait: true
        }
      }
    }
  }

  post {
    success { echo "taskhub-ci-dev SUCCESS (IMAGE_TAG=${env.IMAGE_TAG})" }
    failure { echo "taskhub-ci-dev FAILED" }
  }
}