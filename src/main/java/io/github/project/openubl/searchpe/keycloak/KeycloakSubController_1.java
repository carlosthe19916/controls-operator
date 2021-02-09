package io.github.project.openubl.searchpe.keycloak;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
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
public class KeycloakSubController_1 implements SubController {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @ResourcePath("keycloak/secret.yaml")
    Template secretTemplate;

    @Inject
    @ResourcePath("keycloak/ingress.yaml")
    Template ingressTemplate;

    @Inject
    @ResourcePath("keycloak/service.yaml")
    Template serviceTemplate;

    @Override
    public void createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context) {
        String ns = searchpe.getMetadata().getNamespace();
        String name = searchpe.getMetadata().getName();

        // Process templates

        String secretValue = secretTemplate.data(
                "name", name,
                "adminPassword", RandomStringUtils.randomAlphabetic(10)
        ).render();

        String serviceTemplateValue = serviceTemplate.data(
                "name", name
        ).render();

        String ingressTemplateValue = ingressTemplate.data(
                "name", name,
                "host", ""
        ).render();

        // Parse template values to k8s objects

        Secret secret = Serialization.unmarshal(secretValue, Secret.class);
        Ingress ingress = Serialization.unmarshal(ingressTemplateValue, Ingress.class);
        Service service = Serialization.unmarshal(serviceTemplateValue, Service.class);

        secret.getMetadata().setNamespace(ns);
        ingress.getMetadata().setNamespace(ns);
        service.getMetadata().setNamespace(ns);

        //

        if (kubernetesClient.secrets().inNamespace(ns).withName(secret.getMetadata().getName()).get() == null) {
            kubernetesClient.secrets().inNamespace(ns).createOrReplace(secret);
        }

        if (kubernetesClient.services().inNamespace(ns).withName(service.getMetadata().getName()).get() == null) {
            kubernetesClient.services().inNamespace(ns).createOrReplace(service);
        }

        if (kubernetesClient.network().v1().ingresses().inNamespace(ns).withName(ingress.getMetadata().getName()).get() == null) {
            kubernetesClient.network().v1().ingresses().inNamespace(ns).createOrReplace(ingress);
        }
    }

}
