package application.hub;

import application.Realm;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class HubViewModel {
    public static class CardVM {
        public final ObjectProperty<Realm> realm = new SimpleObjectProperty<>();
        public final IntegerProperty level = new SimpleIntegerProperty(1);
        public final IntegerProperty xpInto = new SimpleIntegerProperty(0);
        public final IntegerProperty xpToNext = new SimpleIntegerProperty(100);
    }
    private final ObservableList<CardVM> cards = FXCollections.observableArrayList();

    public ObservableList<CardVM> getCards() { return cards; }
}