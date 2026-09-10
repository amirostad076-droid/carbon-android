abstract final class InstanceConstants {
  static const int apiCodeVersion = 1;
  static const String defaultApiBaseUrl = 'https://fluxer.arashyn.ir/api';
  static const String defaultInstanceInputUrl = 'fluxer.arashyn.ir';
  static const String defaultMarketingBaseUrl = 'https://fluxer.arashyn.ir';
  static const String defaultProductName = 'CARBON';
  static const int maxRecentInstances = 5;

  static const Set<String> officialInstanceHosts = <String>{
    'fluxer.arashyn.ir',
    'fluxer.app',
    'web.fluxer.app',
    'api.fluxer.app',
    'canary.fluxer.app',
    'web.canary.fluxer.app',
    'api.canary.fluxer.app',
    'fluxer.com',
    'web.fluxer.com',
    'api.fluxer.com',
    'canary.fluxer.com',
    'web.canary.fluxer.com',
    'api.canary.fluxer.com',
  };
}
