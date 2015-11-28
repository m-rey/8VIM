package inc.flide.eightvim;

import android.inputmethodservice.InputMethodService;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import inc.flide.eightvim.keyboardHelpers.FingerPosition;
import inc.flide.eightvim.keyboardHelpers.KeyboardAction;
import inc.flide.logging.Logger;

public class EightVimInputMethodService extends InputMethodService {

    EightVimKeyboardView eightVimKeyboardView;
    private boolean isShiftPressed;
    private boolean isCapsLockOn;
    Map<List<FingerPosition>, KeyboardAction> keyboardActionMap;

    @Override
    public View onCreateInputView() {
        eightVimKeyboardView = new EightVimKeyboardView(this);
        return eightVimKeyboardView;
    }


    @Override
    public void onStartInput (EditorInfo attribute, boolean restarting){
        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onStartInputView (EditorInfo info, boolean restarting){
        super.onStartInputView(info, restarting);
    }

    @Override
    public void onInitializeInterface(){
        super.onInitializeInterface();
        initializeKeyboardActionMap();
    }

    /** Helper to commit text to input */
    public void sendText(String str) {
        getCurrentInputConnection().commitText(str, 1);
    }

    /** Helper to send a special key to input */
    public void sendKey(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void initializeKeyboardActionMap() {

        InputStream inputStream = null;
        try{
            inputStream = getResources().openRawResource(getResources().getIdentifier("raw/keyboard_actions", "raw", getPackageName()));
            KeyboardActionXmlParser keyboardActionXmlParser = new KeyboardActionXmlParser(inputStream);
            keyboardActionMap = keyboardActionXmlParser.readKeyboardActionMap();

        } catch (XmlPullParserException exception){
            exception.printStackTrace();
        } catch (IOException exception){
            exception.printStackTrace();
        } catch(Exception exception){
            exception.printStackTrace();
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void processMovementSequence(List<FingerPosition> movementSequence) {

        KeyboardAction keyboardAction = keyboardActionMap.get(movementSequence);
        boolean isMovementValid = true;
        if(keyboardAction == null){
            Logger.Warn(this, "No Action Mapping has been defined for the given Sequence : " + movementSequence.toString());
            movementSequence.clear();
            return;
        }

        switch (keyboardAction.getKeyboardActionType()){
            case INPUT_TEXT:
                handleInputText(keyboardAction);
                break;
            case INPUT_KEY:
                handleInputKey(keyboardAction);
                break;
            case INPUT_SPECIAL:
                handleSpecialInput(keyboardAction);
                break;
            default:
                Logger.Warn(this, "Action Type Undefined : " + keyboardAction.getKeyboardActionType().toString());
                isMovementValid = false;
                break;
        }
        if(isMovementValid){
            eightVimKeyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    private void handleInputText(KeyboardAction keyboardAction) {
        if(keyboardAction.getText().length() == 1 && (isShiftPressed || isCapsLockOn)){
            sendText(keyboardAction.getText().toUpperCase());
            isShiftPressed = false;
        }else{
            sendText(keyboardAction.getText());
        }
    }

    private void handleInputKey(KeyboardAction keyboardAction) {
        sendKey(keyboardAction.getKeyEventCode());
    }

    private void handleSpecialInput(KeyboardAction keyboardAction) {
        switch (keyboardAction.getKeyEventCode()){
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                if(isShiftPressed){
                    isShiftPressed = false;
                    isCapsLockOn = true;
                } else if(isCapsLockOn){
                    isShiftPressed = false;
                    isCapsLockOn = false;
                } else{
                    isShiftPressed = true;
                    isCapsLockOn = false;
                }
                break;
            default:
                Logger.Warn(this, "Special Event undefined for keyCode : " + keyboardAction.getKeyEventCode());
                break;
        }
    }
}