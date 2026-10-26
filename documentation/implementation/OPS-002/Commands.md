### Apply 
```shell
 kubectl --context minikube -n bio-dashboard apply -f devops/kubernetes/frontend
```

### Get Pods
```shell
kubectl --context minikube -n bio-dashboard get pods
```

### Get deployment
```shell
kubectl --context minikube -n bio-dashboard get deployments -o 'jsonpath={.items[*].metadata.name}'
```

### logs
```shell
kubectl --context minikube -n bio-dashboard logs deployment/auth-service --tail=18
```

### Get ready deployment
```shell
kubectl --context minikube -n bio-dashboard get deployments -o 'custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,AVAILABLE:.status.availableReplicas'
```

### Get events
```shell
kubectl --context minikube -n bio-dashboard get events --field-selector type=Warning --sort-by=.lastTimestamp
```


### Forward port
```shell
kubectl --context minikube -n bio-dashboard port-forward --address=127.0.0.1 svc/frontend 18080:80
```