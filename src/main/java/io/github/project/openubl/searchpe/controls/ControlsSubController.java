package io.github.project.openubl.searchpe.controls;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.github.project.openubl.searchpe.Searchpe;
import io.github.project.openubl.searchpe.SubController;
import io.javaoperatorsdk.operator.api.Context;
import io.quarkus.qute.Template;
import io.quarkus.qute.api.ResourcePath;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ControlsSubController implements SubController {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @ResourcePath("controls/service.yaml")
    Template serviceTemplate;

    @Override
    public void createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context) {
        String ns = searchpe.getMetadata().getNamespace();
        String name = searchpe.getMetadata().getName();

        // Process templates

        String serviceTemplateValue = serviceTemplate.data(
                "name", name
        ).render();

        // Parse template values to k8s objects

        Service service = Serialization.unmarshal(serviceTemplateValue, Service.class);
        service.getMetadata().setNamespace(ns);

        //

        if (kubernetesClient.services().inNamespace(ns).withName(service.getMetadata().getName()).get() == null) {
            kubernetesClient.services().inNamespace(ns).createOrReplace(service);
        }

    }

}
