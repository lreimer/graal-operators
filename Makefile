NAME = graal-operators
VERSION = 1.0.0
GCP = gcloud
ZONE = europe-west1-b
K8S = kubectl

.PHONY: info

info:
	@echo "Kubernetes Operators with GraalVM"

prepare:
	@$(GCP) config set compute/zone $(ZONE)
	@$(GCP) config set container/use_client_certificate False

cluster:
	@echo "Create GKE Cluster"
	# --[no-]enable-basic-auth --[no-]issue-client-certificate

	@$(GCP) container clusters create $(NAME) --num-nodes=5 --enable-autoscaling --min-nodes=5 --max-nodes=10 --machine-type=n1-standard-4 --enable-stackdriver-kubernetes --enable-ip-alias --enable-autorepair --scopes cloud-platform --addons HorizontalPodAutoscaling,HttpLoadBalancing --cluster-version "1.14"
	@$(K8S) create clusterrolebinding cluster-admin-binding --clusterrole=cluster-admin --user=$$(gcloud config get-value core/account)
	@$(K8S) cluster-info

test-killpod-operator:
	@$(K8S) apply -f killpod-operator/src/test/resources/

gcloud-login:
	@$(GCP) auth application-default login

access-token:
	@$(GCP) config config-helper --format=json | jq .credential.access_token

destroy:
	@$(GCP) container clusters delete $(NAME) --async --quiet
