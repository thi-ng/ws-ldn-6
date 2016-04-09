// http://glslsandbox.com/e#23379.3
// https://www.shadertoy.com/view/llXGD2

float smin( float a, float b, float k ) {
  float res = exp( -k*a ) + exp( -k*b );
  return -log( res )/k;
}

mat2 rot (float an) {
  return mat2(cos(an), -sin(an),
              sin(an), cos(an));
}

float scene (vec3 pos) {

  pos.x = mod(pos.x, 6.0);
  pos.y = mod(pos.y, 6.0);
  pos.z = mod(pos.z, 6.0);

  //float timeAngle = mod(iGlobalTime, 2.0*3.141592);
  //float an = pos.y*0.01;
  //pos.xz *= m;

  float thick = 0.5;
  vec3 d =  abs(pos - vec3(3.0, 3.0, 3.0)) - vec3(thick, 3.1, thick);
  vec3 de = abs(pos - vec3(3.0, 3.0, 3.0)) - vec3(3.1, thick, thick);
  vec3 dee = abs(pos - vec3(3.0, 3.0, 3.0)) - vec3(thick, thick, 3.1);

  float a = min(max(d.x,max(d.y,d.z)),0.0) +
    length(max(d,0.0));

  float b = min(max(de.x,max(de.y,de.z)),0.0) +
    length(max(de,0.0));
  float c = min(max(dee.x,max(dee.y,dee.z)),0.0) +
    length(max(dee,0.0));

  return smin(a, smin(b, c, 2.0), 2.0);
}

vec3 normal (vec3 pos) {
  vec2 r = vec2(0.001, 0.0);
  return normalize(vec3(scene(pos-r.xyy) - scene(pos+r.xyy),
                        scene(pos-r.yxy) - scene(pos+r.yxy),
                        scene(pos-r.yyx) - scene(pos+r.yyx)
                        ));
}

void mainImage(vec2 uv, vec2 aspect) {
  uv = uv * 2.0 - 1.0;
  uv.x *= resolution.x / resolution.y;

  vec3 color = vec3(0.5);

  vec3 ro = vec3(uv, time);
  vec3 rd = normalize(vec3(uv, 1.0));
  float timeAngle = mod(time, 2.0*3.141592);
  rd.xz *= rot(sin(timeAngle) * 0.5);
  rd.yz *= rot(cos(timeAngle) * 0.5);

  float inte = 0.0;
  for (int i = 0; i < 128; ++i) {
    vec3 pos = ro + rd*inte;
    float t = scene(pos);
    inte += max(0.01, t);
    if (t < 0.0) {
      vec3 n = normal(pos);
      float direct = mix(0.125, 1.0, max(0.0, dot(n, normalize(vec3(0.0, -2.0, 1.0)) )));
      color = vec3(1.0) * direct;
      color = mix(vec3(0.5), color, exp(-length(pos-ro)*0.1));
      break;
    }
  }

  gl_FragColor = vec4(pow(color, vec3(1.0/2.0)), 1.0);
}
