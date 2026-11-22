package com.fasdeq.airtok;

final class GlassShader {
    private GlassShader() {}
    static final String LENS = """

            uniform shader content;
            uniform float  margin;
            uniform float2 viewSize;
            uniform float4 shape1;    // 真实主形状（触摸凸起的尺度用它）
            uniform float4 radii1;
            uniform float4 shape1L;   // 主形状：平边方向延伸到视图外，那条边就没有斜面（视图矩形外硬切）
            uniform float  rimSoft;   // 边缘柔化：折射带内沿法线方向的抹匀宽度（px，0 = 关）
            uniform float4 shape2;
            uniform float  radius2;
            uniform float  blendK;
            uniform float  bevel;
            uniform float  refractPx;
            uniform float  falloff;      // 折射剖面：> 0 为逆幂（引力透镜）衰减指数，0 = 平方斜面
            uniform float  refractDir;   // -1 向内采样（默认，与 iOS 一致：内侧压缩镜像）/ +1 向外（可选的凸透镜模式）
            uniform float2 sampleLo;     // 采样安全区（录制内容坐标）：区外没有内容，读到的是透明黑
            uniform float2 sampleHi;
            uniform float  dispersion;
            uniform float2 lightDir;
            uniform float  specStrength;
            uniform float  rimBandMax;   // 贴边高光带宽度上限（px，小控件按短边收）
            uniform float4 tintColor;
            uniform float  adaptiveTint;
            uniform float4 glassTint;
            uniform float  dimAmount;
            uniform float  satFactor;
            uniform float  press;
            uniform float2 touchPos;
            uniform float  touchAmp;

            float sdRoundedBox(float2 p, float2 b, float r) {
                float2 q = abs(p) - b + r;
                return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
            }

            float sdRoundedBox4(float2 p, float2 b, float4 r) {
                float rx = (p.x > 0.0) ? ((p.y > 0.0) ? r.z : r.y) : ((p.y > 0.0) ? r.w : r.x);
                float2 q = abs(p) - b + rx;
                return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - rx;
            }

            float sminPoly(float a, float b, float k) {
                float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
                return mix(b, a, h) - k * h * (1.0 - h);
            }

            float lensSDF(float2 p) {
                float d = sdRoundedBox4(p - shape1L.xy, shape1L.zw, radii1);
                if (shape2.z > 0.5) {
                    float d2 = sdRoundedBox(p - shape2.xy, shape2.zw, radius2);
                    if (blendK > 0.5) {
                        d = sminPoly(d, d2, blendK);
                    } else {
                        d = min(d, d2);
                    }
                }
                return d;
            }

            half4 main(float2 coord) {
                float2 p = coord - float2(margin, margin);
                if (p.x < 0.0 || p.y < 0.0 || p.x > viewSize.x || p.y > viewSize.y) {
                    return half4(0.0);
                }
                float d = lensSDF(p);

                float cov = clamp(0.5 - d / 1.5, 0.0, 1.0);
                if (cov <= 0.004) {
                    return half4(0.0);
                }

                float2 n = float2(
                    lensSDF(p + float2(1.0, 0.0)) - lensSDF(p - float2(1.0, 0.0)),
                    lensSDF(p + float2(0.0, 1.0)) - lensSDF(p - float2(0.0, 1.0))
                );
                float nLen = length(n);
                if (nLen > 0.0001) {
                    n = n / nLen;
                } else {
                    n = float2(0.0, -1.0);
                }

                float t = clamp(-d / max(bevel, 1.0), 0.0, 1.0);
                float edge = 1.0 - t;
                float slope;
                if (falloff > 0.001) {
                    float gB = pow(5.0, -falloff);
                    slope = (pow(1.0 + 4.0 * t, -falloff) - gB) / (1.0 - gB);
                } else {
                    slope = edge * edge;
                }

                float refr = refractPx * (1.0 + 0.6 * press);
                float2 offset = n * (refractDir * slope * refr);

                if (touchAmp > 0.001) {
                    float2 tp = p - touchPos;
                    float tr = length(tp);
                    float sigma = max(max(shape1.z, shape1.w), 1.0);
                    float bump = touchAmp * exp(-(tr * tr) / (sigma * sigma * 0.30));
                    if (tr > 1.0) {
                        offset -= (tp / tr) * (bump * refr * 0.5);
                    }
                }

                float2 cR = coord + offset * (1.0 - dispersion * slope);
                float2 cG = coord + offset;
                float2 cB = coord + offset * (1.0 + dispersion * slope);

                float2 lo = sampleLo;
                float2 hi = sampleHi;
                cR = clamp(cR, lo, hi);
                cG = clamp(cG, lo, hi);
                cB = clamp(cB, lo, hi);
                float3 col;
                if (rimSoft > 0.01 && slope > 0.001) {
                    float2 sm = n * (rimSoft * slope);
                    float2 cR1 = clamp(cR - sm, lo, hi);
                    float2 cR2 = clamp(cR + sm, lo, hi);
                    float2 cG1 = clamp(cG - sm, lo, hi);
                    float2 cG2 = clamp(cG + sm, lo, hi);
                    float2 cB1 = clamp(cB - sm, lo, hi);
                    float2 cB2 = clamp(cB + sm, lo, hi);
                    col = float3(
                        (content.eval(cR).r + content.eval(cR1).r + content.eval(cR2).r) / 3.0,
                        (content.eval(cG).g + content.eval(cG1).g + content.eval(cG2).g) / 3.0,
                        (content.eval(cB).b + content.eval(cB1).b + content.eval(cB2).b) / 3.0
                    );
                } else {
                    col = float3(
                        content.eval(cR).r,
                        content.eval(cG).g,
                        content.eval(cB).b
                    );
                }

                float lum = dot(col, float3(0.2126, 0.7152, 0.0722));
                if (satFactor <= 1.0) {
                    col = mix(float3(lum), col, satFactor);
                } else {
                    float satNow = max(col.r, max(col.g, col.b)) - min(col.r, min(col.g, col.b));
                    float room = 1.0 - smoothstep(0.2, 0.85, satNow);
                    float hl = 1.0 - smoothstep(0.75, 0.98, lum);
                    float amount = 1.0 + (satFactor - 1.0) * mix(0.3, 1.0, room * hl);
                    col = clamp(mix(float3(lum), col, amount), float3(0.0), float3(1.0));
                }

                if (adaptiveTint > 0.5) {
                    float lumT = dot(col, float3(0.2126, 0.7152, 0.0722));
                    float e = smoothstep(0.35, 0.75, lumT);
                    col = mix(col, float3(1.0 - e), 0.14 + 0.08 * e);
                } else {
                    col = mix(col, tintColor.rgb, tintColor.a);
                }
                col = col * (1.0 - dimAmount);

                if (glassTint.a > 0.002) {
                    float lumTint = dot(col, float3(0.2126, 0.7152, 0.0722));
                    float3 absorbed = col * mix(float3(1.0), glassTint.rgb, 0.85);
                    float3 scattered = glassTint.rgb * (0.38 * (1.0 - lumTint));
                    col = mix(col, clamp(absorbed + scattered, float3(0.0), float3(1.0)), glassTint.a);
                }

                float facing = dot(n, -lightDir);
                float lobeF = pow(max(facing, 0.0), 4.5);
                float lobeB = pow(max(-facing, 0.0), 4.5);

                float bandW = clamp(bevel * 0.3, 2.0, rimBandMax);
                float glowIn = clamp((-d - 1.0) / 2.0, 0.0, 1.0);
                float glow = glowIn * pow(clamp(1.0 - (-d - 3.0) / bandW, 0.0, 1.0), 1.5) * cov;
                float hair = clamp(1.0 - abs(d + 1.0) / 2.0, 0.0, 1.0) * cov;
                float spec = (hair * 0.70 * (lobeF + lobeB) + glow * 0.10 * lobeF)
                             * specStrength * (1.0 - 0.35 * press);
                col += float3(spec);

                col = clamp(col, float3(0.0), float3(1.0));
                return half4(half3(col * cov), half(cov));
            }

""";
}
