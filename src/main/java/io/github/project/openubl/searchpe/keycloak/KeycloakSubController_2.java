package io.github.project.openubl.searchpe.keycloak;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
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
public class KeycloakSubController_2 implements SubController {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @ResourcePath("keycloak/cm.yaml")
    Template cmTemplate;

    @Inject
    @ResourcePath("keycloak/deployment.yaml")
    Template deploymentTemplate;

    @Override
    public void createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context) {
        String ns = searchpe.getMetadata().getNamespace();
        String name = searchpe.getMetadata().getName();

        // Process templates

        String cmValue = cmTemplate.data(
                "name", name
        ).render();

        String deploymentValue = deploymentTemplate.data(
                "name", name
        ).render();

        // Parse template values to k8s objects

        ConfigMap cm = Serialization.unmarshal(cmValue, ConfigMap.class);
        Deployment deployment = Serialization.unmarshal(deploymentValue, Deployment.class);

        cm.getMetadata().setNamespace(ns);
        deployment.getMetadata().setNamespace(ns);

        //

        if (kubernetesClient.configMaps().inNamespace(ns).withName(cm.getMetadata().getName()).get() == null) {
            kubernetesClient.configMaps().inNamespace(ns).createOrReplace(cm);
        }

        kubernetesClient.apps().deployments().inNamespace(ns).createOrReplace(deployment);
    }

}
