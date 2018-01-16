package com.example.lch.zxing;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.lch.zxing.zxing.CaptureActivity;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    /**
     * 扫一扫按钮点击事件
     * @param view  view
     */
    public void scanClick(View view) {
        permissionCheck();      //获取权限

        //注：申请权限不一定要用这个库，你可以自己写，如果只想参考扫码功能，则如下
        //startActivity(new Intent(this, CaptureActivity.class));       //启动扫码页面
    }

    /**
     * 权限申请（相机权限）
     * 参考https://github.com/yanzhenjie/AndPermission
     */
    private void permissionCheck() {
        AndPermission.with(this)
                .requestCode(200)
                .permission(Permission.CAMERA)
                .rationale(new RationaleListener() {
                    @Override
                    public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                        AndPermission.rationaleDialog(MainActivity.this, rationale).show();
                    }
                })
                .callback(listener)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 400: { // 这个400就是上面defineSettingDialog()的第二个参数。
                // 你可以在这里检查你需要的权限是否被允许，并做相应的操作。
                startActivity(new Intent(this, CaptureActivity.class));
                break;
            }
        }
    }

    private PermissionListener listener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, List<String> grantedPermissions) {
            // 权限申请成功回调。

            // 这里的requestCode就是申请时设置的requestCode。
            // 和onActivityResult()的requestCode一样，用来区分多个不同的请求。
            if(requestCode == 200) {
                startActivity(new Intent(MainActivity.this, CaptureActivity.class));
            }
        }

        @Override
        public void onFailed(int requestCode, List<String> deniedPermissions) {
            // 权限申请失败回调。
            if(requestCode == 200) {
                // 是否有不再提示并拒绝的权限。
                if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, deniedPermissions)) {
                    AndPermission.defaultSettingDialog(MainActivity.this, 400)
                            .setTitle("权限申请失败")
                            .setMessage("您拒绝了我们必要的一些权限，已经没法愉快的玩耍了，请在设置中授权！")
                            .setPositiveButton("好，去设置")
                            .show();

                }
            }
        }
    };
}
