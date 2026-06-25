module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires java.prefs;
    requires miglayout.swing;
    requires com.google.gson;
    requires com.google.common;
    requires org.slf4j;

    opens com.udacity.catpoint.data to com.google.gson;
}
