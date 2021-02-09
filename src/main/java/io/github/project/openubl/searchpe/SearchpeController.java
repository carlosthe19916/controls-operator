package io.github.project.openubl.searchpe;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.*;
import org.jboss.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SearchpeController implements ResourceController<Searchpe> {

    private static final Logger log = Logger.getLogger(SearchpeController.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    Instance<SubController> visitors;

    @Override
    public UpdateControl<Searchpe> createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context) {
        for (SubController v : visitors) {
            v.createOrUpdateResource(searchpe, context);
        }

        String ns = searchpe.getMetadata().getNamespace();

        Map<String, String> data = new HashMap<>();
        data.put("index.html", searchpe.getSpec().getHtml());

        ConfigMap htmlConfigMap = new ConfigMapBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(configMapName(searchpe))
                                .withNamespace(ns)
                                .build()
                )
                .withData(data)
                .build();

        SearchpeStatus status = new SearchpeStatus();
        status.setHtmlConfigMap(htmlConfigMap.getMetadata().getName());
        status.setAreWeGood("Yes!");
        searchpe.setStatus(status);

        return UpdateControl.updateCustomResource(searchpe);
    }

    @Override
    public DeleteControl deleteResource(Searchpe nginx, io.javaoperatorsdk.operator.api.Context<Searchpe> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    private static String configMapName(Searchpe nginx) {
        return nginx.getMetadata().getName() + "-html";
    }

    private static String deploymentName(Searchpe nginx) {
        return nginx.getMetadata().getName();
    }

    private static String serviceName(Searchpe nginx) {
        return nginx.getMetadata().getName();
    }

}
