package cn.shyman.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import cn.shyman.library.scan.OnScanListener;
import cn.shyman.library.scan.ScanView;

public class SplashActivity extends AppCompatActivity {
	private ScanView scanView;
	private ImageView ivFlashMode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		this.scanView = (ScanView) findViewById(R.id.scanView);
		this.scanView.setOnScanListener(new OnScanListener() {
			@Override
			public void scanSuccess(String result) {
				Toast.makeText(SplashActivity.this, result, Toast.LENGTH_SHORT).show();
			}
		});
		this.ivFlashMode = (ImageView) findViewById(R.id.ivFlashMode);
		this.ivFlashMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				scanView.setFlashMode(!scanView.isFlashMode());
				ivFlashMode.setSelected(scanView.isFlashMode());
			}
		});
		this.ivFlashMode.setSelected(this.scanView.isFlashMode());
		
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		this.scanView.startScan();
	}
	
	@Override
	protected void onPause() {
		this.scanView.stopScan();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 1) {
			if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				this.scanView.initScan();
			}
			return;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}
}
