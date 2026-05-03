bag::CoreConfig ToCoreConfig(int sample_rate_hz, int frame_samples,
                             int enable_diagnostics, bag_transport_mode mode,
                             bag_flash_signal_profile flash_signal_profile,
                             bag_flash_voicing_flavor flash_voicing_flavor,
                             int reserved) {
  bag::CoreConfig core_config{};
  core_config.sample_rate_hz = sample_rate_hz;
  core_config.frame_samples = frame_samples;
  core_config.enable_diagnostics = enable_diagnostics != 0;
  switch (mode) {
    case BAG_TRANSPORT_FLASH:
      core_config.mode = bag::TransportMode::kFlash;
      break;
    case BAG_TRANSPORT_PRO:
      core_config.mode = bag::TransportMode::kPro;
      break;
    case BAG_TRANSPORT_ULTRA:
      core_config.mode = bag::TransportMode::kUltra;
      break;
    case BAG_TRANSPORT_MINI:
      core_config.mode = bag::TransportMode::kMini;
      break;
    default:
      core_config.mode = static_cast<bag::TransportMode>(-1);
      break;
  }
  switch (flash_signal_profile) {
    case BAG_FLASH_SIGNAL_PROFILE_STEADY:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kSteady;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_LITANY:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kLitany;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_HOSTILE:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kHostile;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_COLLAPSE:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kCollapse;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_ZEAL:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kZeal;
      break;
    case BAG_FLASH_SIGNAL_PROFILE_VOID:
      core_config.flash_signal_profile = bag::FlashSignalProfile::kVoid;
      break;
    default:
      core_config.flash_signal_profile =
          static_cast<bag::FlashSignalProfile>(-1);
      break;
  }
  switch (flash_voicing_flavor) {
    case BAG_FLASH_VOICING_FLAVOR_STEADY:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kSteady;
      break;
    case BAG_FLASH_VOICING_FLAVOR_LITANY:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kLitany;
      break;
    case BAG_FLASH_VOICING_FLAVOR_HOSTILE:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kHostile;
      break;
    case BAG_FLASH_VOICING_FLAVOR_COLLAPSE:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kCollapse;
      break;
    case BAG_FLASH_VOICING_FLAVOR_ZEAL:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kZeal;
      break;
    case BAG_FLASH_VOICING_FLAVOR_VOID:
      core_config.flash_voicing_flavor = bag::FlashVoicingFlavor::kVoid;
      break;
    default:
      core_config.flash_voicing_flavor =
          static_cast<bag::FlashVoicingFlavor>(-1);
      break;
  }
  core_config.reserved = reserved;
  return core_config;
}

bag_transport_mode ToApiMode(bag::TransportMode mode) {
  switch (mode) {
    case bag::TransportMode::kFlash:
      return BAG_TRANSPORT_FLASH;
    case bag::TransportMode::kPro:
      return BAG_TRANSPORT_PRO;
    case bag::TransportMode::kUltra:
      return BAG_TRANSPORT_ULTRA;
    case bag::TransportMode::kMini:
      return BAG_TRANSPORT_MINI;
    default:
      return BAG_TRANSPORT_FLASH;
  }
}
