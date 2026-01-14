pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    timestamps()
  }

  parameters {
    booleanParam(
      name: 'SETUP_MONITORING',
      defaultValue: false,
      description: 'Install/upgrade kube-prometheus-stack + RunRun monitoring manifests (requires Jenkins credential: slack-webhook-url).'
    )
  }

  triggers {
    githubPush()
  }

  environment {
    AWS_REGION       = "mx-central-1"
    EKS_CLUSTER_NAME = "terraform-eks-cluster"

    ECR_REPO_URI     = "118320467932.dkr.ecr.mx-central-1.amazonaws.com/terraform-ecr"

    IMAGE_TAG        = "${BUILD_NUMBER}"
  }

  stages {
    stage("Checkout") {
      steps {
        checkout scm
      }
    }

    stage("Gradle Build") {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail
chmod +x gradlew
./gradlew clean build -x test
'''
      }
    }

    stage("Docker Build & Push (ECR)") {
      steps {
        withCredentials([[
          $class: 'AmazonWebServicesCredentialsBinding',
          credentialsId: 'aws-credentials'
        ]]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${ECR_REPO_URI%/*}"

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${REGISTRY}"

docker build -t "${ECR_REPO_URI}:${IMAGE_TAG}" .
docker push "${ECR_REPO_URI}:${IMAGE_TAG}"

docker tag "${ECR_REPO_URI}:${IMAGE_TAG}" "${ECR_REPO_URI}:latest"
docker push "${ECR_REPO_URI}:latest"
'''
        }
      }
    }

    stage("Configure kubeconfig") {
      steps {
        withCredentials([[
          $class: 'AmazonWebServicesCredentialsBinding',
          credentialsId: 'aws-credentials'
        ]]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail
aws eks update-kubeconfig --region "${AWS_REGION}" --name "${EKS_CLUSTER_NAME}"
kubectl get nodes
'''
        }
      }
    }

    stage("Setup Monitoring (Prometheus/Grafana/Alertmanager)") {
      when {
        expression { return params.SETUP_MONITORING }
      }
      steps {
        withCredentials([
          [
            $class: 'AmazonWebServicesCredentialsBinding',
            credentialsId: 'aws-credentials'
          ],
          string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK_URL')
        ]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail

kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

kubectl -n monitoring create secret generic alertmanager-slack \
  --from-literal=SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL}" \
  --dry-run=client -o yaml | kubectl apply -f -

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
  -n monitoring \
  -f monitoring/kube-prometheus-stack-values.yaml

kubectl apply -f k8s/monitoring/runrun-servicemonitor.yaml
kubectl apply -f k8s/monitoring/runrun-prometheusrule.yaml
kubectl apply -f k8s/monitoring/grafana-ingress.yaml

kubectl -n monitoring get pods
'''
        }
      }
    }

    stage("Deploy Infra (External Redis/Mongo Atlas)") {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail

echo "[skip] Redis: using ElastiCache (no in-cluster Redis)"
echo "[skip] MongoDB: using MongoDB Atlas (no in-cluster MongoDB)"
'''
      }
    }

    stage("Deploy App (Deployment/Service/Ingress)") {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail

command -v envsubst >/dev/null 2>&1 || {
  echo "envsubst not found. Install gettext-base (e.g. sudo apt-get install -y gettext-base)" >&2
  exit 1
}

envsubst < k8s/eks_deploy_alb.yml | kubectl apply -f -

kubectl rollout status deployment/runrun -n default --timeout=300s
kubectl get pods -o wide
kubectl get svc -o wide
kubectl get ingress -o wide || true
'''
      }
    }

    stage("Verify ALB Controller") {
      steps {
        sh '''#!/usr/bin/env bash
set -euo pipefail
if [ -x "./check-alb.sh" ]; then
  ./check-alb.sh
else
  echo "[skip] check-alb.sh not found or not executable"
fi
'''
      }
    }
  }

  post {
    always {
      sh '''#!/usr/bin/env bash
set -euo pipefail
docker image prune -f || true
'''
    }
  }
}
