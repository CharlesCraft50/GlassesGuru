precision mediump float;

uniform sampler2D u_Texture;

uniform vec4 u_LightingParameters;
uniform vec4 u_MaterialParameters;
uniform vec4 u_ColorCorrectionParameters;

#if USE_DEPTH_FOR_OCCLUSION
uniform sampler2D u_DepthTexture;
uniform mat3 u_DepthUvTransform;
uniform float u_DepthAspectRatio;
#endif // USE_DEPTH_FOR_OCCLUSION

varying vec3 v_ViewPosition;
varying vec3 v_ViewNormal;
varying vec2 v_TexCoord;
varying vec3 v_ScreenSpacePosition;
uniform vec4 u_ObjColor;
uniform vec4 u_CustomColor;
uniform float u_Transparency;

#if USE_DEPTH_FOR_OCCLUSION

float DepthGetMillimeters(in sampler2D depth_texture, in vec2 depth_uv) {
  // Depth is packed into the red and green components of its texture.
  // The texture is a normalized format, storing millimeters.
  vec3 packedDepthAndVisibility = texture2D(depth_texture, depth_uv).xyz;
  return dot(packedDepthAndVisibility.xy, vec2(255.0, 256.0 * 255.0));
}

// Function definitions...

#endif // USE_DEPTH_FOR_OCCLUSION

void main() {
    // We support approximate sRGB gamma.
    const float kGamma = 0.4545454;
    const float kInverseGamma = 2.2;
    const float kMiddleGrayGamma = 0.466;

    // Unpack lighting and material parameters for better naming.
    vec3 viewLightDirection = u_LightingParameters.xyz;
    vec3 colorShift = u_ColorCorrectionParameters.rgb;
    float averagePixelIntensity = u_ColorCorrectionParameters.a;

    float materialAmbient = u_MaterialParameters.x;
    float materialDiffuse = u_MaterialParameters.y;
    float materialSpecular = u_MaterialParameters.z;
    float materialSpecularPower = u_MaterialParameters.w;

    // Normalize varying parameters, because they are linearly interpolated in the vertex shader.
    vec3 viewFragmentDirection = normalize(v_ViewPosition);
    vec3 viewNormal = normalize(v_ViewNormal);

    // Flip the y-texture coordinate to address the texture from top-left.
    vec4 objectColor = texture2D(u_Texture, vec2(v_TexCoord.x, 1.0 - v_TexCoord.y));

    // Apply color to grayscale image only if the alpha of u_ObjColor is
    // greater and equal to 255.0.
    objectColor.rgb *= mix(vec3(1.0), u_ObjColor.rgb / 255.0, step(255.0, u_ObjColor.a));

    // Apply inverse SRGB gamma to the texture before making lighting calculations.
    objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));

    // Check if a custom color has been set
    bool isCustomColorSet = u_CustomColor.a > 0.0;

    // Use the custom color if it has been set, otherwise use the texture color
    objectColor.rgb = isCustomColorSet ? u_CustomColor.rgb : objectColor.rgb;

    // Ambient light is unaffected by the light intensity.
    float ambient = materialAmbient;

    // Approximate a hemisphere light (not a harsh directional light).
    float diffuse = materialDiffuse *
                    0.5 * (dot(viewNormal, viewLightDirection) + 1.0);

    // Compute specular light. Textures are loaded with with premultiplied alpha
    // (https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inPremultiplied),
    // so premultiply the specular color by alpha as well.
    vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);
    float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));
    float specular = objectColor.a * materialSpecular *
                     pow(specularStrength, materialSpecularPower);

    // Apply transparency to the lighting terms
    if (u_Transparency > 0.0) {
        diffuse *= u_Transparency;
        specular *= u_Transparency;
    }

    vec3 color = objectColor.rgb * (ambient + diffuse) + specular;
    // Apply SRGB gamma before writing the fragment color.
    color.rgb = pow(color, vec3(kGamma));
    // Apply average pixel intensity and color shift
    color *= colorShift * (averagePixelIntensity / kMiddleGrayGamma);
    gl_FragColor.rgb = color;

    // Apply transparency to the final color only if u_Transparency is greater than 0
    if (u_Transparency > 0.0) {
        color.rgb *= u_Transparency;
        gl_FragColor.a = objectColor.a * u_Transparency;
    } else {
        gl_FragColor.a = objectColor.a;
    }

#if USE_DEPTH_FOR_OCCLUSION
    const float kMetersToMillimeters = 1000.0;
    float asset_depth_mm = v_ViewPosition.z * kMetersToMillimeters * -1.;
    // Computes the texture coordinates to sample from the depth image.
    vec2 depth_uvs = (u_DepthUvTransform * vec3(v_ScreenSpacePosition.xy, 1)).xy;

    // The following step is very costly. Replace the last line with the
    // commented line if it's too expensive.
    // gl_FragColor *= DepthGetVisibility(u_DepthTexture, depth_uvs, asset_depth_mm);
    gl_FragColor *= DepthGetBlurredVisibilityAroundUV(u_DepthTexture, depth_uvs, asset_depth_mm);
#endif // USE_DEPTH_FOR_OCCLUSION
}
