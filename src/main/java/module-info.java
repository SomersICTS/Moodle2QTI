module ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires java.xml;
    requires stax.utils;
    requires java.desktop;
    requires java.net.http;

    opens ui to javafx.fxml;
    exports ui;
}