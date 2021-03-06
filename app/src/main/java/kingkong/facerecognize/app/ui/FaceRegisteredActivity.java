package kingkong.facerecognize.app.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import kingkong.facerecognize.app.R;

/**
 * Created by KingKong-HE on 2017/12/26.
 * 人脸验证界面
 * @author KingKong-HE
 * @Time 2017/12/26
 * @Email 709872217@QQ.COM
 */
public class FaceRegisteredActivity extends AppCompatActivity{

    private CameraView cameraViewID;

    private ImageView btuCommit;

    private Toolbar toolbarID;

    // 进度对话框
    private ProgressDialog mProDialog;

    //采用身份识别接口进行在线人脸识别
    private IdentityVerifier mIdVerifier;

    private String login_name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognize);

        login_name = getIntent().getStringExtra("login_name");

        cameraViewID = (CameraView)findViewById(R.id.cameraViewID);
        toolbarID = findViewById(R.id.toolbar);
        btuCommit = findViewById(R.id.btuCommit);

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setTitle("请稍后");

        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                // cancel进度框时,取消正在进行的操作
                if (null != mIdVerifier) {
                    mIdVerifier.cancel();
                }
            }
        });

        toolbarID.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mIdVerifier = IdentityVerifier.createVerifier(FaceRegisteredActivity.this, new InitListener() {
            @Override
            public void onInit(int errorCode) {
                if (ErrorCode.SUCCESS == errorCode) {
                    Toast.makeText(FaceRegisteredActivity.this,"引擎初始化成功",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FaceRegisteredActivity.this,"引擎初始化失败，错误码：" + errorCode,Toast.LENGTH_SHORT).show();
                }
            }
        });

        cameraViewID.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                Bitmap bitmap = cameraKitImage.getBitmap();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //可根据流量及网络状况对图片进行压缩
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] mImageData = baos.toByteArray();
                getVerification(mImageData);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        btuCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraViewID.captureImage();
            }
        });

    }

    //设置注册信息
    public void getVerification(byte[] mImageData){
        if (null != mImageData && mImageData.length > 0) {
            mProDialog.setMessage("注册中...");
            mProDialog.show();
            // 设置用户标识，格式为6-18个字符（由字母、数字、下划线组成，不得以数字开头，不能包含空格）。
            // 当不设置时，云端将使用用户设备的设备ID来标识终端用户。
            // 设置人脸注册参数
            // 清空参数
            mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
            // 设置会话场景
            mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ifr");
            // 设置会话类型
            mIdVerifier.setParameter(SpeechConstant.MFV_SST, "enroll");
            // 设置用户id
            mIdVerifier.setParameter(SpeechConstant.AUTH_ID, login_name);
            // 设置监听器，开始会话
            mIdVerifier.startWorking(mEnrollListener);

            // 子业务执行参数，若无可以传空字符传
            StringBuffer params = new StringBuffer();
            // 向子业务写入数据，人脸数据可以一次写入
            mIdVerifier.writeData("ifr", params.toString(), mImageData, 0, mImageData.length);
            // 停止写入
            mIdVerifier.stopWrite("ifr");
        } else {
            Toast.makeText(FaceRegisteredActivity.this,"请选择图片后再验证" ,Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 人脸注册监听器
     */
    private IdentityListener mEnrollListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d("king---", result.getResultString());

            if (null != mProDialog) {
                mProDialog.dismiss();
            }

            try {
                JSONObject object = new JSONObject(result.getResultString());
                int ret = object.getInt("ret");

                if (ErrorCode.SUCCESS == ret) {
                    setResult(21);
                    finish();
                    Toast.makeText(FaceRegisteredActivity.this,"注册成功" ,Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(FaceRegisteredActivity.this,"注册失败:"
                            + new SpeechError(ret).getPlainDescription(true) ,Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        @Override
        public void onError(SpeechError error) {
            if (null != mProDialog) {
                mProDialog.dismiss();
            }

            Toast.makeText(FaceRegisteredActivity.this,error.getPlainDescription(true) ,Toast.LENGTH_SHORT).show();
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        cameraViewID.start();
    }

    @Override
    protected void onPause() {
        cameraViewID.stop();
        super.onPause();
    }

    @Override
    public void finish() {
        if (null != mProDialog) {
            mProDialog.dismiss();
        }
        super.finish();
    }
}
