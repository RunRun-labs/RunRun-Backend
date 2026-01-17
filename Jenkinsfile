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
      description: 'Install/upgrade kube-prometheus-stack + RunRun monitoring manifests (assumes alertmanager-slack Secret already exists).'
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
        withCredentials([[
          $class: 'AmazonWebServicesCredentialsBinding',
          credentialsId: 'aws-credentials'
        ]]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail

kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

if ! kubectl -n monitoring get secret alertmanager-slack >/dev/null 2>&1; then
  echo "[skip] alertmanager-slack secret not found in namespace monitoring"
  echo "Create it once with:"
  echo "  kubectl -n monitoring create secret generic alertmanager-slack --from-literal=SLACK_WEBHOOK_URL='https://hooks.slack.com/services/XXX/YYY/ZZZ'"
  exit 0
fi

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
        withCredentials([[
          $class: 'AmazonWebServicesCredentialsBinding',
          credentialsId: 'aws-credentials'
        ]]) {
          sh '''#!/usr/bin/env bash
set -euo pipefail

command -v envsubst >/dev/null 2>&1 || {
  echo "envsubst not found. Install gettext-base (e.g. sudo apt-get install -y gettext-base)" >&2
  exit 1
}

		# Ensure runrun-env Secret has the latest MONGO_URI (without overwriting other keys)
		if [ -f ".env.prod" ]; then
		  MONGO_URI_LINE="$(grep -E '^MONGO_URI=' .env.prod | head -n 1 || true)"
		  if [ -n "${MONGO_URI_LINE}" ]; then
		    MONGO_URI_VALUE="${MONGO_URI_LINE#MONGO_URI=}"
		    MONGO_URI_B64="$(printf %s "${MONGO_URI_VALUE}" | base64 | tr -d '\n')"

		    if kubectl -n default get secret runrun-env >/dev/null 2>&1; then
		      kubectl -n default patch secret runrun-env --type='json' \
		        -p="[{\"op\":\"add\",\"path\":\"/data/MONGO_URI\",\"value\":\"${MONGO_URI_B64}\"}]"
		    else
		      kubectl -n default create secret generic runrun-env --from-literal="MONGO_URI=${MONGO_URI_VALUE}"
		    fi
		  else
		    echo "[warn] MONGO_URI not found in .env.prod; skipping runrun-env secret update"
		  fi
		else
		  echo "[warn] .env.prod not found; skipping runrun-env secret update"
		fi

	envsubst < k8s/eks_deploy_alb.yml | kubectl apply -f -

		if ! kubectl rollout status deployment/runrun -n default --timeout=600s; then
		  echo "[error] Rollout did not complete; dumping diagnostics..."
		  kubectl get pods -n default -l app=runrun -o wide || true
		  kubectl describe pods -n default -l app=runrun || true
		  kubectl logs -n default -l app=runrun --all-containers=true --tail=200 || true
		  exit 1
		fi

		kubectl get pods -o wide
		kubectl get svc -o wide
		kubectl get ingress -o wide || true
	'''
        }
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
