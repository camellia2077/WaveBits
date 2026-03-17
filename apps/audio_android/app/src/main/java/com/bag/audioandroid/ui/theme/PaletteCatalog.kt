package com.bag.audioandroid.ui.theme

import androidx.compose.ui.graphics.Color
import com.bag.audioandroid.ui.model.PaletteFamily
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.model.PaletteOption

val MaterialPalettes = listOf(
    vividPalette(
        id = "ruby",
        family = PaletteFamily.RedsPinks,
        titleResId = R.string.palette_ruby_title,
        primary = Color(0xFFC62828),
        primaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFF8B4A47),
        tertiary = Color(0xFF7A5A12),
        background = Color(0xFFFFF8F7),
        dark = darkSeed(
            background = Color(0xFF171112),
            surface = Color(0xFF22181A),
            primary = Color(0xFFFF8A80),
            secondary = Color(0xFFE0B4B0),
            tertiary = Color(0xFFF3C97A)
        )
    ),
    vividPalette(
        id = "crimson",
        family = PaletteFamily.RedsPinks,
        titleResId = R.string.palette_crimson_title,
        primary = Color(0xFFB71C45),
        primaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFF97546A),
        tertiary = Color(0xFF8F5B2D),
        background = Color(0xFFFFF7F9),
        dark = darkSeed(
            background = Color(0xFF170F14),
            surface = Color(0xFF21161D),
            primary = Color(0xFFFF84A2),
            secondary = Color(0xFFE0B1C2),
            tertiary = Color(0xFFF2C47B)
        )
    ),
    vividPalette(
        id = "rose",
        family = PaletteFamily.RedsPinks,
        titleResId = R.string.palette_rose_title,
        primary = Color(0xFFE05A77),
        primaryContainer = Color(0xFFFFD9E0),
        secondary = Color(0xFFA95F74),
        tertiary = Color(0xFF9B6A36),
        background = Color(0xFFFFF8FA),
        dark = darkSeed(
            background = Color(0xFF1A1115),
            surface = Color(0xFF25171C),
            primary = Color(0xFFFF9BB1),
            secondary = Color(0xFFE7B9C5),
            tertiary = Color(0xFFF3C97C)
        )
    ),
    vividPalette(
        id = "sakura",
        family = PaletteFamily.RedsPinks,
        titleResId = R.string.palette_sakura_title,
        primary = Color(0xFFF06292),
        primaryContainer = Color(0xFFFFD9E6),
        secondary = Color(0xFFB36A83),
        tertiary = Color(0xFF8D6A4A),
        background = Color(0xFFFFF8FB),
        dark = darkSeed(
            background = Color(0xFF191116),
            surface = Color(0xFF24181E),
            primary = Color(0xFFFFA8C4),
            secondary = Color(0xFFE9BED0),
            tertiary = Color(0xFFE9CC9E)
        )
    ),
    vividPalette(
        id = "raspberry",
        family = PaletteFamily.RedsPinks,
        titleResId = R.string.palette_raspberry_title,
        primary = Color(0xFFAD1457),
        primaryContainer = Color(0xFFFFD8E6),
        secondary = Color(0xFF8A5568),
        tertiary = Color(0xFF9A5B4C),
        background = Color(0xFFFFF7F9),
        dark = darkSeed(
            background = Color(0xFF170F13),
            surface = Color(0xFF23161C),
            primary = Color(0xFFFF9BC0),
            secondary = Color(0xFFE2B8C8),
            tertiary = Color(0xFFF0C0AF)
        )
    ),
    vividPalette(
        id = "blush",
        family = PaletteFamily.RedsPinks,
        titleResId = R.string.palette_blush_title,
        primary = Color(0xFFF48FB1),
        primaryContainer = Color(0xFFFFD8E5),
        secondary = Color(0xFFB56A7D),
        tertiary = Color(0xFF9A7050),
        background = Color(0xFFFFF8FB),
        dark = darkSeed(
            background = Color(0xFF191116),
            surface = Color(0xFF25181E),
            primary = Color(0xFFFFB0C8),
            secondary = Color(0xFFE8C1CD),
            tertiary = Color(0xFFEBCDA6)
        )
    ),
    vividPalette(
        id = "orange",
        family = PaletteFamily.Oranges,
        titleResId = R.string.palette_orange_title,
        primary = Color(0xFFD95F02),
        primaryContainer = Color(0xFFFFDBC8),
        secondary = Color(0xFF81523C),
        tertiary = Color(0xFF715C2D),
        background = Color(0xFFFFF8F4),
        dark = darkSeed(
            background = Color(0xFF18120E),
            surface = Color(0xFF241A15),
            primary = Color(0xFFFFA55D),
            secondary = Color(0xFFE1B79D),
            tertiary = Color(0xFFF0CE82)
        )
    ),
    vividPalette(
        id = "coral",
        family = PaletteFamily.Oranges,
        titleResId = R.string.palette_coral_title,
        primary = Color(0xFFFF6F61),
        primaryContainer = Color(0xFFFFDAD3),
        secondary = Color(0xFF9C5E58),
        tertiary = Color(0xFF8A6648),
        background = Color(0xFFFFF8F6),
        dark = darkSeed(
            background = Color(0xFF1A110F),
            surface = Color(0xFF261915),
            primary = Color(0xFFFFAA9F),
            secondary = Color(0xFFE5BBB4),
            tertiary = Color(0xFFF0D09A)
        )
    ),
    vividPalette(
        id = "tangerine",
        family = PaletteFamily.Oranges,
        titleResId = R.string.palette_tangerine_title,
        primary = Color(0xFFEB7A00),
        primaryContainer = Color(0xFFFFE0B2),
        secondary = Color(0xFF8A633A),
        tertiary = Color(0xFF73603D),
        background = Color(0xFFFFF9F3),
        dark = darkSeed(
            background = Color(0xFF19130C),
            surface = Color(0xFF261C13),
            primary = Color(0xFFFFC16D),
            secondary = Color(0xFFE7C29E),
            tertiary = Color(0xFFF0D08E)
        )
    ),
    vividPalette(
        id = "apricot",
        family = PaletteFamily.Oranges,
        titleResId = R.string.palette_apricot_title,
        primary = Color(0xFFFF8A65),
        primaryContainer = Color(0xFFFFDDCF),
        secondary = Color(0xFFA16659),
        tertiary = Color(0xFF8A6A49),
        background = Color(0xFFFFF8F5),
        dark = darkSeed(
            background = Color(0xFF1A120F),
            surface = Color(0xFF261A16),
            primary = Color(0xFFFFB49E),
            secondary = Color(0xFFE6C0B7),
            tertiary = Color(0xFFF0D5A1)
        )
    ),
    vividPalette(
        id = "terracotta",
        family = PaletteFamily.Oranges,
        titleResId = R.string.palette_terracotta_title,
        primary = Color(0xFFBF5B3D),
        primaryContainer = Color(0xFFFFDBCF),
        secondary = Color(0xFF86604F),
        tertiary = Color(0xFF7B6241),
        background = Color(0xFFFFF8F5),
        dark = darkSeed(
            background = Color(0xFF17120F),
            surface = Color(0xFF231B17),
            primary = Color(0xFFE7AD92),
            secondary = Color(0xFFDABFB3),
            tertiary = Color(0xFFE5D1A1)
        )
    ),
    vividPalette(
        id = "persimmon",
        family = PaletteFamily.Oranges,
        titleResId = R.string.palette_persimmon_title,
        primary = Color(0xFFE65100),
        primaryContainer = Color(0xFFFFDBCC),
        secondary = Color(0xFF8A5C4C),
        tertiary = Color(0xFF7E653A),
        background = Color(0xFFFFF8F4),
        dark = darkSeed(
            background = Color(0xFF19110D),
            surface = Color(0xFF251915),
            primary = Color(0xFFFFAB7C),
            secondary = Color(0xFFE3BEB0),
            tertiary = Color(0xFFEECF91)
        )
    ),
    vividPalette(
        id = "amber",
        family = PaletteFamily.Yellows,
        titleResId = R.string.palette_amber_title,
        primary = Color(0xFFA96800),
        primaryContainer = Color(0xFFFFDFAC),
        secondary = Color(0xFF755A2F),
        tertiary = Color(0xFF5C6400),
        background = Color(0xFFFFF9F1),
        dark = darkSeed(
            background = Color(0xFF18140C),
            surface = Color(0xFF241C12),
            primary = Color(0xFFFFB95A),
            secondary = Color(0xFFE2C38E),
            tertiary = Color(0xFFD4DE7A)
        )
    ),
    vividPalette(
        id = "gold",
        family = PaletteFamily.Yellows,
        titleResId = R.string.palette_gold_title,
        primary = Color(0xFFC08A00),
        primaryContainer = Color(0xFFFFE6B8),
        secondary = Color(0xFF816330),
        tertiary = Color(0xFF5E650A),
        background = Color(0xFFFFFAF2),
        dark = darkSeed(
            background = Color(0xFF19150C),
            surface = Color(0xFF261E12),
            primary = Color(0xFFFFC867),
            secondary = Color(0xFFE6CA96),
            tertiary = Color(0xFFD7E585)
        )
    ),
    vividPalette(
        id = "sunflower",
        family = PaletteFamily.Yellows,
        titleResId = R.string.palette_sunflower_title,
        primary = Color(0xFF8C7A00),
        primaryContainer = Color(0xFFF8E287),
        secondary = Color(0xFF716538),
        tertiary = Color(0xFF5C6800),
        background = Color(0xFFFFFBEA),
        dark = darkSeed(
            background = Color(0xFF17140B),
            surface = Color(0xFF231C12),
            primary = Color(0xFFE7D067),
            secondary = Color(0xFFD8CCA0),
            tertiary = Color(0xFFCFD978)
        )
    ),
    vividPalette(
        id = "lemon",
        family = PaletteFamily.Yellows,
        titleResId = R.string.palette_lemon_title,
        primary = Color(0xFF9E9100),
        primaryContainer = Color(0xFFFFF0A6),
        secondary = Color(0xFF746A35),
        tertiary = Color(0xFF606800),
        background = Color(0xFFFFFDEA),
        dark = darkSeed(
            background = Color(0xFF17150B),
            surface = Color(0xFF241E12),
            primary = Color(0xFFF4E46F),
            secondary = Color(0xFFDCD29A),
            tertiary = Color(0xFFD8E27C)
        )
    ),
    vividPalette(
        id = "honey",
        family = PaletteFamily.Yellows,
        titleResId = R.string.palette_honey_title,
        primary = Color(0xFFB37A00),
        primaryContainer = Color(0xFFFFDF9A),
        secondary = Color(0xFF7C5E2F),
        tertiary = Color(0xFF696300),
        background = Color(0xFFFFF9EE),
        dark = darkSeed(
            background = Color(0xFF18130B),
            surface = Color(0xFF241C12),
            primary = Color(0xFFFFC56B),
            secondary = Color(0xFFE2C493),
            tertiary = Color(0xFFD4DD7C)
        )
    ),
    vividPalette(
        id = "marigold",
        family = PaletteFamily.Yellows,
        titleResId = R.string.palette_marigold_title,
        primary = Color(0xFFC79200),
        primaryContainer = Color(0xFFFFEBB0),
        secondary = Color(0xFF866530),
        tertiary = Color(0xFF6D6700),
        background = Color(0xFFFFFAF1),
        dark = darkSeed(
            background = Color(0xFF18140B),
            surface = Color(0xFF251D12),
            primary = Color(0xFFFFD070),
            secondary = Color(0xFFE6CC9C),
            tertiary = Color(0xFFDBE484)
        )
    ),
    vividPalette(
        id = "lime",
        family = PaletteFamily.Greens,
        titleResId = R.string.palette_lime_title,
        primary = Color(0xFF5E8E00),
        primaryContainer = Color(0xFFD9F5A0),
        secondary = Color(0xFF58653F),
        tertiary = Color(0xFF3A6D59),
        background = Color(0xFFFBFCEA),
        dark = darkSeed(
            background = Color(0xFF12150C),
            surface = Color(0xFF1B2012),
            primary = Color(0xFFC0E86D),
            secondary = Color(0xFFC7D5A7),
            tertiary = Color(0xFF9BDCC7)
        )
    ),
    vividPalette(
        id = "olive",
        family = PaletteFamily.Greens,
        titleResId = R.string.palette_olive_title,
        primary = Color(0xFF6B7A12),
        primaryContainer = Color(0xFFE2F1AA),
        secondary = Color(0xFF626847),
        tertiary = Color(0xFF4A6C44),
        background = Color(0xFFFBFCEF),
        dark = darkSeed(
            background = Color(0xFF13150E),
            surface = Color(0xFF1D2016),
            primary = Color(0xFFD1E27D),
            secondary = Color(0xFFD0D8AE),
            tertiary = Color(0xFFB5D69C)
        )
    ),
    vividPalette(
        id = "emerald",
        family = PaletteFamily.Greens,
        titleResId = R.string.palette_emerald_title,
        primary = Color(0xFF008A24),
        primaryContainer = Color(0xFF9EF2A8),
        secondary = Color(0xFF486548),
        tertiary = Color(0xFF1F6D68),
        background = Color(0xFFF4FCF4),
        dark = darkSeed(
            background = Color(0xFF0D1510),
            surface = Color(0xFF152018),
            primary = Color(0xFF7EEA8F),
            secondary = Color(0xFFB9D6BA),
            tertiary = Color(0xFF8EDCD4)
        )
    ),
    vividPalette(
        id = "mint",
        family = PaletteFamily.Greens,
        titleResId = R.string.palette_mint_title,
        primary = Color(0xFF1AAE8E),
        primaryContainer = Color(0xFFB7F2E6),
        secondary = Color(0xFF47756B),
        tertiary = Color(0xFF4A6691),
        background = Color(0xFFF3FCFA),
        dark = darkSeed(
            background = Color(0xFF0D1615),
            surface = Color(0xFF15211F),
            primary = Color(0xFF6DE6CA),
            secondary = Color(0xFFB5D8D2),
            tertiary = Color(0xFFA9C8F0)
        )
    ),
    vividPalette(
        id = "jade",
        family = PaletteFamily.Greens,
        titleResId = R.string.palette_jade_title,
        primary = Color(0xFF00A86B),
        primaryContainer = Color(0xFFAAF3D2),
        secondary = Color(0xFF467368),
        tertiary = Color(0xFF466B8C),
        background = Color(0xFFF2FCF8),
        dark = darkSeed(
            background = Color(0xFF0D1613),
            surface = Color(0xFF15211D),
            primary = Color(0xFF6EE6B1),
            secondary = Color(0xFFB5D8CF),
            tertiary = Color(0xFFA8C8EA)
        )
    ),
    vividPalette(
        id = "forest",
        family = PaletteFamily.Greens,
        titleResId = R.string.palette_forest_title,
        primary = Color(0xFF2E7D32),
        primaryContainer = Color(0xFFCAEFCF),
        secondary = Color(0xFF566650),
        tertiary = Color(0xFF2E6A5A),
        background = Color(0xFFF5FBF4),
        dark = darkSeed(
            background = Color(0xFF0E150F),
            surface = Color(0xFF172019),
            primary = Color(0xFF8FD98E),
            secondary = Color(0xFFC0D4BE),
            tertiary = Color(0xFF95D6C7)
        )
    ),
    vividPalette(
        id = "teal",
        family = PaletteFamily.CyansBlues,
        titleResId = R.string.palette_teal_title,
        primary = Color(0xFF00839B),
        primaryContainer = Color(0xFFABEEFF),
        secondary = Color(0xFF496368),
        tertiary = Color(0xFF4F6090),
        background = Color(0xFFF2FCFF),
        dark = darkSeed(
            background = Color(0xFF0C1518),
            surface = Color(0xFF152026),
            primary = Color(0xFF6ADCF8),
            secondary = Color(0xFFB4D0D5),
            tertiary = Color(0xFFABC2F0)
        )
    ),
    vividPalette(
        id = "cyan",
        family = PaletteFamily.CyansBlues,
        titleResId = R.string.palette_cyan_title,
        primary = Color(0xFF00A7C2),
        primaryContainer = Color(0xFFB8F4FF),
        secondary = Color(0xFF4B6870),
        tertiary = Color(0xFF506A97),
        background = Color(0xFFF1FCFF),
        dark = darkSeed(
            background = Color(0xFF0A1418),
            surface = Color(0xFF132026),
            primary = Color(0xFF76E9FF),
            secondary = Color(0xFFB7D6DC),
            tertiary = Color(0xFFACC6F5)
        )
    ),
    vividPalette(
        id = "sky",
        family = PaletteFamily.CyansBlues,
        titleResId = R.string.palette_sky_title,
        primary = Color(0xFF1A7BFF),
        primaryContainer = Color(0xFFD8E7FF),
        secondary = Color(0xFF51617D),
        tertiary = Color(0xFF6A5C8C),
        background = Color(0xFFF5F9FF),
        dark = darkSeed(
            background = Color(0xFF0C131A),
            surface = Color(0xFF14202A),
            primary = Color(0xFF8AB8FF),
            secondary = Color(0xFFC1CEE3),
            tertiary = Color(0xFFD0B8FF)
        )
    ),
    vividPalette(
        id = "ocean",
        family = PaletteFamily.CyansBlues,
        titleResId = R.string.palette_ocean_title,
        primary = Color(0xFF005CE6),
        primaryContainer = Color(0xFFDCE2FF),
        secondary = Color(0xFF505E7A),
        tertiary = Color(0xFF6B5778),
        background = Color(0xFFF7F9FF),
        dark = darkSeed(
            background = Color(0xFF0B121A),
            surface = Color(0xFF131C27),
            primary = Color(0xFF8AB4FF),
            secondary = Color(0xFFBECAE0),
            tertiary = Color(0xFFD3B4DA)
        )
    ),
    vividPalette(
        id = "cobalt",
        family = PaletteFamily.CyansBlues,
        titleResId = R.string.palette_cobalt_title,
        primary = Color(0xFF2563EB),
        primaryContainer = Color(0xFFDCE4FF),
        secondary = Color(0xFF56627D),
        tertiary = Color(0xFF6A5E90),
        background = Color(0xFFF6F9FF),
        dark = darkSeed(
            background = Color(0xFF0C111B),
            surface = Color(0xFF141B29),
            primary = Color(0xFF92B3FF),
            secondary = Color(0xFFC1CAE3),
            tertiary = Color(0xFFD1C1FF)
        )
    ),
    vividPalette(
        id = "indigo",
        family = PaletteFamily.CyansBlues,
        titleResId = R.string.palette_indigo_title,
        primary = Color(0xFF3D4FD4),
        primaryContainer = Color(0xFFDFE0FF),
        secondary = Color(0xFF5A5E7A),
        tertiary = Color(0xFF7A536A),
        background = Color(0xFFF8F8FF),
        dark = darkSeed(
            background = Color(0xFF0F111A),
            surface = Color(0xFF181A27),
            primary = Color(0xFFB2BAFF),
            secondary = Color(0xFFC8CAE0),
            tertiary = Color(0xFFE1B4CC)
        )
    ),
    vividPalette(
        id = "violet",
        family = PaletteFamily.PurplesMagentas,
        titleResId = R.string.palette_violet_title,
        primary = Color(0xFF7A2CF5),
        primaryContainer = Color(0xFFE9DCFF),
        secondary = Color(0xFF695A80),
        tertiary = Color(0xFF8A4E78),
        background = Color(0xFFFCF7FF),
        dark = darkSeed(
            background = Color(0xFF13101A),
            surface = Color(0xFF1E1827),
            primary = Color(0xFFD2B5FF),
            secondary = Color(0xFFD4C4EA),
            tertiary = Color(0xFFF0B6DE)
        )
    ),
    vividPalette(
        id = "plum",
        family = PaletteFamily.PurplesMagentas,
        titleResId = R.string.palette_plum_title,
        primary = Color(0xFF8E44AD),
        primaryContainer = Color(0xFFF0DBFF),
        secondary = Color(0xFF776084),
        tertiary = Color(0xFFA15C6C),
        background = Color(0xFFFDF7FF),
        dark = darkSeed(
            background = Color(0xFF141018),
            surface = Color(0xFF201925),
            primary = Color(0xFFE0B8FF),
            secondary = Color(0xFFD8C7E4),
            tertiary = Color(0xFFF0BDCA)
        )
    ),
    vividPalette(
        id = "magenta",
        family = PaletteFamily.PurplesMagentas,
        titleResId = R.string.palette_magenta_title,
        primary = Color(0xFFC2188F),
        primaryContainer = Color(0xFFFFD7F0),
        secondary = Color(0xFF82526E),
        tertiary = Color(0xFF8D4D4D),
        background = Color(0xFFFFF7FB),
        dark = darkSeed(
            background = Color(0xFF170F15),
            surface = Color(0xFF23171E),
            primary = Color(0xFFFFA8E3),
            secondary = Color(0xFFE0BCD2),
            tertiary = Color(0xFFF0B9B4)
        )
    ),
    vividPalette(
        id = "fuchsia",
        family = PaletteFamily.PurplesMagentas,
        titleResId = R.string.palette_fuchsia_title,
        primary = Color(0xFFE11D8D),
        primaryContainer = Color(0xFFFFD8EC),
        secondary = Color(0xFF92566F),
        tertiary = Color(0xFF92545C),
        background = Color(0xFFFFF7FB),
        dark = darkSeed(
            background = Color(0xFF180F15),
            surface = Color(0xFF24161D),
            primary = Color(0xFFFFA9DD),
            secondary = Color(0xFFE3BDD0),
            tertiary = Color(0xFFF0C0BE)
        )
    ),
    vividPalette(
        id = "orchid",
        family = PaletteFamily.PurplesMagentas,
        titleResId = R.string.palette_orchid_title,
        primary = Color(0xFF9C27B0),
        primaryContainer = Color(0xFFF5D8FF),
        secondary = Color(0xFF7E5B84),
        tertiary = Color(0xFFA45A70),
        background = Color(0xFFFFF7FF),
        dark = darkSeed(
            background = Color(0xFF161019),
            surface = Color(0xFF221925),
            primary = Color(0xFFE8B3FF),
            secondary = Color(0xFFDEC7E2),
            tertiary = Color(0xFFF1BCD0)
        )
    ),
    vividPalette(
        id = "lilac",
        family = PaletteFamily.PurplesMagentas,
        titleResId = R.string.palette_lilac_title,
        primary = Color(0xFFAA7CFF),
        primaryContainer = Color(0xFFEEDDFF),
        secondary = Color(0xFF75608A),
        tertiary = Color(0xFF9A6086),
        background = Color(0xFFFDFAFF),
        dark = darkSeed(
            background = Color(0xFF14111A),
            surface = Color(0xFF201927),
            primary = Color(0xFFDFC2FF),
            secondary = Color(0xFFD8CAE5),
            tertiary = Color(0xFFF0C3DD)
        )
    ),
    vividPalette(
        id = "mocha",
        family = PaletteFamily.Neutrals,
        titleResId = R.string.palette_mocha_title,
        primary = Color(0xFF8B5E3C),
        primaryContainer = Color(0xFFFFDCC2),
        secondary = Color(0xFF786252),
        tertiary = Color(0xFF8A5748),
        background = Color(0xFFFFF8F4),
        dark = darkSeed(
            background = Color(0xFF16120F),
            surface = Color(0xFF211A16),
            primary = Color(0xFFE0B995),
            secondary = Color(0xFFD1C0B1),
            tertiary = Color(0xFFE6B7A9)
        )
    ),
    vividPalette(
        id = "slate",
        family = PaletteFamily.Neutrals,
        titleResId = R.string.palette_slate_title,
        primary = Color(0xFF4B5563),
        primaryContainer = Color(0xFFDDE4EE),
        secondary = Color(0xFF657080),
        tertiary = Color(0xFF6E6383),
        background = Color(0xFFF7F8FB),
        dark = darkSeed(
            background = Color(0xFF111317),
            surface = Color(0xFF191C21),
            primary = Color(0xFFB8C2D1),
            secondary = Color(0xFFC8CFDB),
            tertiary = Color(0xFFD3C5EA)
        )
    ),
    vividPalette(
        id = "sand",
        family = PaletteFamily.Neutrals,
        titleResId = R.string.palette_sand_title,
        primary = Color(0xFF9C7B5B),
        primaryContainer = Color(0xFFF8DDC8),
        secondary = Color(0xFF7E6855),
        tertiary = Color(0xFF8B6248),
        background = Color(0xFFFFF9F5),
        dark = darkSeed(
            background = Color(0xFF17120F),
            surface = Color(0xFF221B17),
            primary = Color(0xFFE7C5A5),
            secondary = Color(0xFFD7C6B8),
            tertiary = Color(0xFFE7BFAE)
        )
    ),
    vividPalette(
        id = "stone",
        family = PaletteFamily.Neutrals,
        titleResId = R.string.palette_stone_title,
        primary = Color(0xFF7A6F63),
        primaryContainer = Color(0xFFE7DED6),
        secondary = Color(0xFF85776C),
        tertiary = Color(0xFF7E6B7F),
        background = Color(0xFFFAF8F6),
        dark = darkSeed(
            background = Color(0xFF121110),
            surface = Color(0xFF1C1A19),
            primary = Color(0xFFCDC1B5),
            secondary = Color(0xFFD4C7BD),
            tertiary = Color(0xFFD8C4D8)
        )
    ),
    vividPalette(
        id = "graphite",
        family = PaletteFamily.Neutrals,
        titleResId = R.string.palette_graphite_title,
        primary = Color(0xFF374151),
        primaryContainer = Color(0xFFD9E2F1),
        secondary = Color(0xFF5E6978),
        tertiary = Color(0xFF6F647F),
        background = Color(0xFFF7F8FB),
        dark = darkSeed(
            background = Color(0xFF101317),
            surface = Color(0xFF171C22),
            primary = Color(0xFFAFBBCB),
            secondary = Color(0xFFC5CEDA),
            tertiary = Color(0xFFD2C7E3)
        )
    ),
    vividPalette(
        id = "ivory",
        family = PaletteFamily.Neutrals,
        titleResId = R.string.palette_ivory_title,
        primary = Color(0xFFA89B8C),
        primaryContainer = Color(0xFFF4E6D7),
        secondary = Color(0xFF8B7C6F),
        tertiary = Color(0xFF907566),
        background = Color(0xFFFFFCF8),
        dark = darkSeed(
            background = Color(0xFF151311),
            surface = Color(0xFF1F1B18),
            primary = Color(0xFFE1D1C1),
            secondary = Color(0xFFD8CCC2),
            tertiary = Color(0xFFE2CDC6)
        )
    )
)

val DefaultMaterialPalette: PaletteOption =
    MaterialPalettes.firstOrNull { it.id == "indigo" } ?: MaterialPalettes.first()
