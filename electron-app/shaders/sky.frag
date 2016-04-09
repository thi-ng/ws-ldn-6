// based on http://glslsandbox.com/e#31148.0
float hash(float n) { return fract(sin(n) * 758.5453); }

float noise(vec3 x) {
  vec3 p = floor(x);
  vec3 f = fract(x);
  // f = f * f * (3.0 - 2.0 * f);
  float n = p.x + p.y * 57.0 + p.z * 800.0;
  return mix(mix(mix(hash(n), hash(n + 1.0), f.x), mix(hash(n + 57.0), hash(n + 58.0), f.x), f.y),
             mix(mix(hash(n + 800.0), hash(n + 801.0), f.x), mix(hash(n + 857.0), hash(n + 858.0), f.x), f.y), f.z);
}

float fbm(vec3 p) {
  float f = 0.0;
  f += 0.50000 * noise(p); p *= 2.02;
  f -= 0.25000 * noise(p); p *= 2.03;
  f += 0.12500 * noise(p); p *= 2.01;
  f += 0.06250 * noise(p); p *= 2.04;
  f -= 0.03125 * noise(p);
  return f / 0.984375;
}

float cloud(vec3 p) {
  p -= fbm(vec3(p.x, p.y, 0.0) * 0.5) * 2.25;
  float a = max(0.0, -(fbm(p * 3.0) * 2.2 - 1.1));
  return a * a;
}

vec3 f2(vec3 c) {
  c += hash(gl_FragCoord.x + gl_FragCoord.y * 9.9) * 0.01;
  c *= 0.7 - length(gl_FragCoord.xy / resolution.xy - mpos) * 0.5;
  float w = length(c);
  return mix(c * vec3(1.0, 1.0, 1.6), vec3(1.4, 1.2, 1.0) * w, w * 1.1 - 0.2);
}

void mainImage(vec2 pos, vec2 aspect) {
  pos.y += 0.2;
  vec2 coord = vec2((pos.x - 0.5) / pos.y, 1.0 / (pos.y + 0.2));
  // coord += fbm(vec3(coord * 18.0, time * 0.001)) * 0.07;
  coord += time * 0.1;
  float q = cloud(vec3(coord, 0.222));
  vec3 col = vec3(0.2, 0.7, 0.8) + vec3(0.2, 0.4, 0.1) * q;
  gl_FragColor = vec4(f2(col), 1.0);
}
