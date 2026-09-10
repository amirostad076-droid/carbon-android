import 'package:flutter/widgets.dart';
import 'package:fluxer_app/core/constants/assets.dart';

class FluxerBrandLogo extends StatelessWidget {
  const FluxerBrandLogo({
    required this.size,
    this.backgroundColor,
    this.symbolColor,
    super.key,
  });

  final double size;
  final Color? backgroundColor;
  final Color? symbolColor;

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: SizedBox(
        width: size,
        height: size,
        child: ClipOval(
          child: Image.asset(
            Assets.carbonSymbol,
            width: size,
            height: size,
            fit: BoxFit.cover,
          ),
        ),
      ),
    );
  }
}
