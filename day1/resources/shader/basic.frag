void mainImage(vec2 pos, vec2 aspect) {
  float d = length((mpos - pos) * aspect);
  float l = 1.0 - d * 4.0;
  vec3 col = vec3(l);
  fragCol = vec4(col, 1.0);
}
