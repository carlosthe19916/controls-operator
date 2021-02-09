package io.github.project.openubl.searchpe.controls;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.github.project.openubl.searchpe.Searchpe;
import io.github.project.openubl.searchpe.SubController;
import io.javaoperatorsdk.operator.api.Context;
import io.quarkus.qute.Template;
import io.quarkus.qute.api.ResourcePath;
import org.apache.commons.lang3.RandomStringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ControlsDBSubController implements SubController {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @ResourcePath("controls-db/secret.yaml")
    Template secretTemplate;

    @Inject
    @ResourcePath("controls-db/pvc.yaml")
    Template pvcTemplate;

    @Inject
    @ResourcePath("controls-db/service.yaml")
    Template serviceTemplate;

    @Inject
    @ResourcePath("controls-db/deployment.yaml")
    Template deploymentTemplate;

    @Override
    public void createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context) {
        String ns = searchpe.getMetadata().getNamespace();
        String name = searchpe.getMetadata().getName();

        // Process templates

        String secretValue = secretTemplate.data(
                "name", name,
                "password", RandomStringUtils.randomAlphabetic(10)
        ).render();

        String pvcTemplateValue = pvcTemplate.data(
                "name", name,
                "capacity", "1Gi"
        ).render();

        String serviceTemplateValue = serviceTemplate.data(
                "name", name
        ).render();

        String deploymentTemplateValue = deploymentTemplate.data(
                "name", name
        ).render();

        // Parse template values to k8s objects

        Secret secret = Serialization.unmarshal(secretValue, Secret.class);
        PersistentVolumeClaim pvc = Serialization.unmarshal(pvcTemplateValue, PersistentVolumeClaim.class);
        Service service = Serialization.unmarshal(serviceTemplateValue, Service.class);
        Deployment deployment = Serialization.unmarshal(deploymentTemplateValue, Deployment.class);

        secret.getMetadata().setNamespace(ns);
        pvc.getMetadata().setNamespace(ns);
        service.getMetadata().setNamespace(ns);
        deployment.getMetadata().setNamespace(ns);

        //

        if (kubernetesClient.secrets().inNamespace(ns).withName(secret.getMetadata().getName()).get() == null) {
            kubernetesClient.secrets().inNamespace(ns).createOrReplace(secret);
        }

        if (kubernetesClient.persistentVolumeClaims().inNamespace(ns).withName(pvc.getMetadata().getName()).get() == null) {
            kubernetesClient.persistentVolumeClaims().inNamespace(ns).createOrReplace(pvc);
        }

        if (kubernetesClient.services().inNamespace(ns).withName(service.getMetadata().getName()).get() == null) {
            kubernetesClient.services().inNamespace(ns).createOrReplace(service);
        }

        kubernetesClient.apps().deployments().inNamespace(ns).createOrReplace(deployment);
    }

}
