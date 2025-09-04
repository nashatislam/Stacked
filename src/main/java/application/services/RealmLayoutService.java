package application.services;

import application.Realm;
import java.net.URL;
import java.util.List;

/** Supplies realm map assets and normalized node specs with thresholds and URLs. */
public interface RealmLayoutService {

    /** Normalized node + threshold unlock + optional resource URL. */
    record NodeSpec(
            String id,
            double x,
            double y,
            String title,
            String tooltip,
            String styleClass,
            int thresholdXp,
            String url
    ) {}

    record Layout(URL mapUrl, URL nodeUrl, List<NodeSpec> nodes) {}

    Layout loadLayout(Realm realm);
}