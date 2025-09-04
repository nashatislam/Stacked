package application.realm;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.URL;

/** Minimal VM if you want to inject/test independently. */
public class RealmMapViewModel {
    public static final class NodeVM {
        private final StringProperty id = new SimpleStringProperty();
        private final DoubleProperty x = new SimpleDoubleProperty();
        private final DoubleProperty y = new SimpleDoubleProperty();
        private final StringProperty title = new SimpleStringProperty();
        private final StringProperty tooltip = new SimpleStringProperty();
        private final StringProperty styleClass = new SimpleStringProperty("node-unlocked");

        public String getId() { return id.get(); }
        public void setId(String v) { id.set(v); }
        public double getX() { return x.get(); }
        public void setX(double v) { x.set(v); }
        public double getY() { return y.get(); }
        public void setY(double v) { y.set(v); }
        public String getTitle() { return title.get(); }
        public void setTitle(String v) { title.set(v); }
        public String getTooltip() { return tooltip.get(); }
        public void setTooltip(String v) { tooltip.set(v); }
        public String getStyleClass() { return styleClass.get(); }
        public void setStyleClass(String v) { styleClass.set(v); }
    }

    private final ObjectProperty<URL> mapUrl = new SimpleObjectProperty<>();
    private final ObjectProperty<URL> nodeUrl = new SimpleObjectProperty<>();
    private final ObservableList<NodeVM> nodes = FXCollections.observableArrayList();

    public URL getMapUrl() { return mapUrl.get(); }
    public void setMapUrl(URL u) { mapUrl.set(u); }
    public URL getNodeUrl() { return nodeUrl.get(); }
    public void setNodeUrl(URL u) { nodeUrl.set(u); }
    public ObservableList<NodeVM> getNodes() { return nodes; }
}
