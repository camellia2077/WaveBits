package com.bag.audioandroid.ui.model

enum class SampleFlavor {
    SacredMachine,
    AncientDynasty,
    ImmortalRot,
    ScarletCarnage,
    ExquisiteFall,
    LabyrinthOfMutability,
}

fun effectiveSampleFlavor(
    themeStyle: ThemeStyleOption,
    brandTheme: BrandThemeOption,
): SampleFlavor =
    if (themeStyle == ThemeStyleOption.BrandDualTone) {
        brandTheme.sampleFlavor
    } else {
        SampleFlavor.SacredMachine
    }
