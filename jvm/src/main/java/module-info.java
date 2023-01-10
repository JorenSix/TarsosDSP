module TarsosDSP.jvm {
    exports be.tarsos.dsp.io.jvm;
    exports be.tarsos.dsp.ui;
    exports be.tarsos.dsp.ui.layers;
    exports be.tarsos.dsp.ui.layers.pch;

    requires java.desktop;
    requires java.logging;

    requires TarsosDSP.core;
}