// http://glslsandbox.com/e#23135.0

//---------------------------------------------------------
// Shader:   SuperNovaeFusion.glsl            by   I.G.P
// original: https://www.shadertoy.com/view/4tfGRr
// use mouse to rotate and look around
//---------------------------------------------------------

//---------------------------------------------------------
vec2 cmul( vec2 a, vec2 b )
{
  return vec2( a.x*b.x - a.y*b.y, a.x*b.y + a.y*b.x );
}

vec3 dmul( vec3 a, vec3 b )
{
  float r = length(a);
  b.xy = cmul(normalize(a.xy), b.xy);
  b.yz = cmul(normalize(a.yz), b.yz);
  b.xz += 0.3 * cmul(normalize(a.xz), b.xz);
  return r*b;
}

float field(in vec3 p)
{
  float res = 0.0;
  vec3 c = p;
  for (int i = 0; i < 10; ++i)
    {
      p = abs(p) / dot(p,p) - 1.0;
      p = dmul(p,p) + 0.7;
      res += exp(-6.0 * abs(dot(p,c)-0.15));
    }
  return max(0.0, res / 3.0);
}

vec3 raycast( in vec3 ro, vec3 rd )
{
  float t = 1.5;
  float dt = 0.25;
  vec3 col = vec3(0.0);
  for( int i=20; i<25; i++ )
    {
      float c = field(ro+t*rd);
      t+=dt / (0.35+c*c);
      c = max(4.0 * c - .9, 0.0);
      c = c*c*c*c;
      col = 0.04*col + 0.04*vec3(0.1*c*c, 0.2*c, 0.0);
    }
  return col;
}

void mainImage(vec2 q, vec2 aspect) {
  vec2 p = -1.0 + 2.0 * q;
  p.x *= resolution.x / resolution.y;
  p += vUV - 0.5;

  // camera
  float angle = 0.05*time + 3.14 * mpos.x;
  vec3 ro = vec3(3.2*cos(angle) + 0.5, 0.0 , 3.2*sin(angle) +0.5);
  vec3 ta = vec3(0.0, 1.2 - 2.0*mpos.y, 0.0);
  vec3 ww = normalize (ta - ro );
  vec3 uu = normalize (cross (ww, vec3(0.0, 1.0, 1.0)));
  vec3 vv = normalize (cross (uu, ww));
  vec3 rd = normalize (p.x*uu + p.y*vv + 4.0*ww);

  // raymarch
  vec3 col = raycast(ro, rd);

  // shade
  col =  0.5 *(log(1.0+0.2*col));
  gl_FragColor = vec4(sqrt(col), 1.0);
}
