package io.github.project.openubl.searchpe;

import io.javaoperatorsdk.operator.api.Context;

public interface SubController {

    void createOrUpdateResource(Searchpe searchpe, Context<Searchpe> context);

}
