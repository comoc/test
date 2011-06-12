var TeapotDemo = function() 
{
  var c      = document.getElementById("macton-teapot-canvas");
  var width  = c.width;
  var height = c.height;
  var gl     = WebGLUtils.setupWebGL(c);

  if (!gl) return;

  var TeapotBumpReflectAttributeBindings = null;
  
  // Could manually set these bindings. But using best guess function gives same result.
  // var TeapotBumpReflectAttributeBindings =
  // { 
  //   Positions: 'g_Position', 
  //   Texcoords: 'g_TexCoord0', 
  //   Tangents:  'g_Tangent', 
  //   Binormals: 'g_Binormal',
  //   Normals:   'g_Normal'
  // };
  
  var bump_reflect_program = null;
  var teapot_model         = null;
  var bump_texture         = null;
  var env_texture          = null;
  
  // The "model" matrix is the "world" matrix in Standard Annotations
  // and Semantics
  var model      = new Matrix4x4();
  var view       = new Matrix4x4();
  var projection = new Matrix4x4();
  var controller = null;
  
//  var bump_reflect_program_config = 
//  {
//    VertexProgramURL:   'shaders/bump_reflect.vs',
//    FragmentProgramURL: 'shaders/bump_reflect.fs',
//  };
//  

  var bump_reflect_program_config = 
  {
    VertexProgramSource: 
		'attribute vec3 g_Position;\
		attribute vec3 g_TexCoord0;\
		attribute vec3 g_Tangent;\
		attribute vec3 g_Binormal;\
		attribute vec3 g_Normal;\
		uniform mat4 world;\
		uniform mat4 worldInverseTranspose;\
		uniform mat4 worldViewProj;\
		uniform mat4 viewInverse;\
		varying vec2 texCoord;\
		varying vec3 worldEyeVec;\
		varying vec3 worldNormal;\
		varying vec3 worldTangent;\
		varying vec3 worldBinorm;\
		void main() \
		{\
		  gl_Position   = worldViewProj * vec4(g_Position.xyz, 1.);\
		  texCoord.xy   = g_TexCoord0.xy;\
		  worldNormal   = (worldInverseTranspose * vec4(g_Normal, 1.)).xyz;\
		  worldTangent  = (worldInverseTranspose * vec4(g_Tangent, 1.)).xyz;\
		  worldBinorm   = (worldInverseTranspose * vec4(g_Binormal, 1.)).xyz;\
		  vec3 worldPos = (world * vec4(g_Position, 1.)).xyz;\
		  worldEyeVec   = normalize(worldPos - viewInverse[3].xyz);\
		}',
    FragmentProgramSource: 
		'#ifdef GL_ES\
		precision highp float;\
		#endif\
		const float bumpHeight = 0.2;\
		uniform sampler2D   normalSampler;\
		uniform samplerCube envSampler;\
		varying vec2 texCoord;\
		varying vec3 worldEyeVec;\
		varying vec3 worldNormal;\
		varying vec3 worldTangent;\
		varying vec3 worldBinorm;\
		void main() \
		{\
		  vec2 bump     = (texture2D(normalSampler, texCoord.xy).xy * 2.0 - 1.0) * bumpHeight;\
		  vec3 normal   = normalize(worldNormal);\
		  vec3 tangent  = normalize(worldTangent);\
		  vec3 binormal = normalize(worldBinorm);\
		  vec3 nb       = normal + bump.x * tangent + bump.y * binormal;\
		  nb = normalize(nb);\
		  vec3 worldEye = normalize(worldEyeVec);\
		  vec3 lookup   = reflect(worldEye, nb);\
		  vec4 color    = textureCube(envSampler, lookup);\
		  gl_FragColor = color;\
		}',
  };
  
  var bump_texture_config =
  {
    Type:      'TEXTURE_2D',
    ImageURL:  'images/bump.jpg',
    TexParameters: 
    {
      TEXTURE_MIN_FILTER: 'LINEAR',
      TEXTURE_MAG_FILTER: 'LINEAR',
      TEXTURE_WRAP_S:     'REPEAT',
      TEXTURE_WRAP_T:     'REPEAT'
    },
    PixelStoreParameters:
    {
      UNPACK_FLIP_Y_WEBGL: true
    },
  };
  
  var env_texture_config =
  {
    Type: 'TEXTURE_CUBE_MAP',
    ImageURL: 
    [ 
      'images/skybox-posx.jpg',
      'images/skybox-negx.jpg',
      'images/skybox-posy.jpg',
      'images/skybox-negy.jpg',
      'images/skybox-posz.jpg',
      'images/skybox-negz.jpg' 
    ],
    ImageType: 
    [ 
      'TEXTURE_CUBE_MAP_POSITIVE_X',
      'TEXTURE_CUBE_MAP_NEGATIVE_X',
      'TEXTURE_CUBE_MAP_POSITIVE_Y',
      'TEXTURE_CUBE_MAP_NEGATIVE_Y',
      'TEXTURE_CUBE_MAP_POSITIVE_Z',
      'TEXTURE_CUBE_MAP_NEGATIVE_Z'
    ],
    TexParameters: 
    {
      TEXTURE_WRAP_S:     'CLAMP_TO_EDGE',
      TEXTURE_WRAP_T:     'CLAMP_TO_EDGE',
      TEXTURE_MIN_FILTER: 'LINEAR',
      TEXTURE_MAG_FILTER: 'LINEAR'
    },
    PixelStoreParameters:
    {
      UNPACK_FLIP_Y_WEBGL: false
    }
  };

  var shaders_loaded      = false;
  var model_loaded        = false;
  var bump_texture_loaded = false;
  var env_texture_loaded  = false

  var TryMain = function()
  {
    if ( shaders_loaded && model_loaded && bump_texture_loaded && env_texture_loaded ) 
    {
      TeapotBumpReflectAttributeBindings = bump_reflect_program.CreateBestVertexBindings( teapot_model ); 
      main();
    }
  }

  var ProgramLoaded = function()
  {
    shaders_loaded = true;
    TryMain();
  }

  var ModelLoaded = function()
  {
    model_loaded = true;
    TryMain();
  }
  
  var bump_textureLoaded = function()
  {
    bump_texture_loaded = true;
    TryMain();
  }

  var env_textureLoaded = function()
  {
    env_texture_loaded = true;
    TryMain();
  }

  bump_reflect_program = new $.glProgram( gl, bump_reflect_program_config, ProgramLoaded     );
  teapot_model         = new $.glModel(   gl, 'models/teapot.json',      ModelLoaded       );
  bump_texture         = new $.glTexture( gl, bump_texture_config,         bump_textureLoaded );
  env_texture          = new $.glTexture( gl, env_texture_config,          env_textureLoaded  );
  
  function main() 
  {
    controller = new CameraController(c);
    // Try the following (and uncomment the "pointer-events: none;" in
    // the index.html) to try the more precise hit detection
    //  controller = new CameraController(document.getElementById("body"), c, gl);
    controller.onchange = function(xRot, yRot) {
        draw();
    };

    gl.enable(gl.DEPTH_TEST);
    gl.clearColor(0.0, 0.0, 0.0, 0.0);

    draw();
  }
  
  function draw() 
  {
    // Note: the viewport is automatically set up to cover the entire Canvas.
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    // Set up the model, view and projection matrices
    projection.loadIdentity();
    projection.perspective(45, width / height, 10, 500);
    view.loadIdentity();
    view.translate(0, -10, -100.0);

    // Add in camera controller's rotation
    model.loadIdentity();
    model.rotate(controller.xRot, 1, 0, 0);
    model.rotate(controller.yRot, 0, 1, 0);

    // Correct for initial placement and orientation of model
    model.translate(0, -10, 0);
    model.rotate(90, 1, 0, 0);

    bump_reflect_program.Use();

    // Compute necessary matrices
    var mvp = new Matrix4x4();
    mvp.multiply(model);
    mvp.multiply(view);
    mvp.multiply(projection);
    var worldInverseTranspose = model.inverse();
    worldInverseTranspose.transpose();
    var viewInverse = view.inverse();

    bump_reflect_program.BindUniform( 'world',                 model.elements );
    bump_reflect_program.BindUniform( 'worldInverseTranspose', worldInverseTranspose.elements );
    bump_reflect_program.BindUniform( 'worldViewProj',         mvp.elements );
    bump_reflect_program.BindUniform( 'viewInverse',           viewInverse.elements );
    bump_reflect_program.BindUniform( 'normalSampler',         bump_texture );
    bump_reflect_program.BindUniform( 'envSampler',            env_texture.Texture );

    bump_reflect_program.BindModel( teapot_model, TeapotBumpReflectAttributeBindings );
    bump_reflect_program.DrawModel();

    $.glCheckError( gl, output );
  }
  
  function output(str) 
  {
    document.body.appendChild(document.createTextNode(str));
    document.body.appendChild(document.createElement("br"));
  }
}

$(document).ready( TeapotDemo );
