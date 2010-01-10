package i4nc4mp.customLock;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.KeyguardManager.OnKeyguardExitResult;
import android.content.Context;
import android.util.Log;

public class ManageKeyguard {
  private static KeyguardManager myKM = null;
  private static KeyguardLock myKL = null;
  
  public static final String TAG = "kg";

  public static synchronized void initialize(Context context) {
    if (myKM == null) {
      myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
      Log.v("MKinit","we had to get the KM.");
    }
  }

  public static synchronized void disableKeyguard(Context context) {
    // myKM = (KeyguardManager)
    // context.getSystemService(Context.KEYGUARD_SERVICE);
    initialize(context);

    if (myKM.inKeyguardRestrictedInputMode()) {
      myKL = myKM.newKeyguardLock(TAG);
      myKL.disableKeyguard();
      //Log.v(TAG,"--Keyguard disabled");
    } else {
      myKL = null;
    }
  }

  //this checks if the keyguard is even on... it actually can't distinguish between password mode or not

  //security is very bad for myLock
  public static synchronized boolean inKeyguardRestrictedInputMode() {
    if (myKM != null) {
      //Log.v(TAG,"--inKeyguardRestrictedInputMode = " + myKM.inKeyguardRestrictedInputMode());
      return myKM.inKeyguardRestrictedInputMode();
    }
    Log.v("KGcheckfail","The MK couldn't see myKM to do the check");
    return false;
  }

  public static synchronized void reenableKeyguard() {
    if (myKM != null) {
      if (myKL != null) {
        myKL.reenableKeyguard();
        myKL = null;
        //Log.v(TAG,"--Keyguard reenabled");
      }
    }
  }

  public static synchronized void exitKeyguardSecurely(final LaunchOnKeyguardExit callback) {
    if (inKeyguardRestrictedInputMode()) {
      Log.v(TAG,"--Trying to exit keyguard securely");
      myKM.exitKeyguardSecurely(new OnKeyguardExitResult() {
        public void onKeyguardExitResult(boolean success) {
          reenableKeyguard();
          if (success) {
            Log.v(TAG,"--Keyguard exited securely");
            callback.LaunchOnKeyguardExitSuccess();
          } else {
            Log.v(TAG,"--Keyguard exit failed");
          }
        }
      });
    } else {
      callback.LaunchOnKeyguardExitSuccess();
    }
  }

  public interface LaunchOnKeyguardExit {
    public void LaunchOnKeyguardExitSuccess();
  }
}
