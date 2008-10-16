/* This file was modified from the original source, please see the
 * zplet.patch file included at the top of this source tree.
 */
/* Zplet, a Z-Machine interpreter in Java */
/* Copyright 1996,2001 Matthew T. Russotto */
/* As of 23 February 2001, this code is open source and covered by the */
/* Artistic License, found within this package */

package russotto.zplet.screenmodel;

import java.util.NoSuchElementException;
import java.util.Vector;

import russotto.zplet.ZColor;
import android.os.Handler;
import android.util.Log;

import com.google.twisty.zplet.Event;
import com.google.twisty.zplet.Font;
import com.google.twisty.zplet.ZMachineException;
import com.google.twisty.zplet.ZViewOutput;

public class ZScreen {
	private final ZViewOutput[] views;
	SyncVector<Integer> inputcodes;
	Vector<Integer> bufferedcodes;
	boolean bufferdone;
	ZWindow inputwindow;
	Font fixedfont;
	Font variablefont;
	Handler dialog_handler;
	int default_foreground;
	int default_background;
	boolean hasscrolled = false;
	final static char accent_table[] = {
		'\u00e4',			/* a-umlaut */
		'\u00f6',			/* o-umlaut */
		'\u00fc',			/* u-umlaut */
		'\u00c4',			/* A-umlaut */
		'\u00d6',			/* O-umlaut */
		'\u00dc',			/* U-umlaut */
		'\u00df',			/* sz-ligature */
		'\u00bb',			/* right-pointing quote */
		'\u00ab',			/* left-pointing quote */
		'\u00eb',			/* e-umlaut */
		'\u00ef',			/* i-umlaut */
		'\u00ff',			/* y-umlaut */
		'\u00cb',			/* E-umlaut */
		'\u00cf',			/* I-umlaut */
		'\u00e1',			/* a-acute */
		'\u00e9',			/* e-acute */
		'\u00ed',			/* i-acute */
		'\u00f3',			/* o-acute */
		'\u00fa',			/* u-acute */
		'\u00fd',			/* y-acute */
		'\u00c1',			/* A-acute */
		'\u00c9',			/* E-acute */
		'\u00cd',			/* I-acute */
		'\u00d3',			/* O-acute */
		'\u00da',			/* U-acute */
		'\u00dd',			/* Y-acute */
		'\u00e0',			/* a-grave */
		'\u00e8',			/* e-grave */
		'\u00ec',			/* i-grave */
		'\u00f2',			/* o-grave */
		'\u00f9',			/* u-grave */
		'\u00c0',			/* A-grave */
		'\u00c8',			/* E-grave */
		'\u00cc',			/* I-grave */
		'\u00d2',			/* O-grave */
		'\u00d9',			/* U-grave */
		'\u00e2',			/* a-circumflex */
		'\u00ea',			/* e-circumflex */
		'\u00ee',			/* i-circumflex */
		'\u00f4',			/* o-circumflex */
		'\u00fb',			/* u-circumflex */
		'\u00c2',			/* A-circumflex */
		'\u00ca',			/* E-circumflex */
		'\u00ce',			/* I-circumflex */
		'\u00d4',			/* O-circumflex */
		'\u00da',			/* U-circumflex */
		'\u00e5',			/* a-ring */
		'\u00c5',			/* A-ring */
		'\u00f8',			/* o-slash */
		'\u00d8',			/* O-slash */
		'\u00e3',			/* a-tilde */
		'\u00f1',			/* n-tilde */
		'\u00f5',			/* o-tilde */
		'\u00c3',			/* A-tilde */
		'\u00d1',			/* N-tilde */
		'\u00d5',			/* O-tilde */
		'\u00e6',			/* ae-ligature */
		'\u00c6',			/* AE-ligature */
		'\u00e7',			/* c-cedilla */
		'\u00c7',			/* C-cedilla */
		'\u00fe',			/* Icelandic thorn */
		'\u00f0',			/* Icelandic eth */
		'\u00de',			/* Icelandic Thorn */
		'\u00d0',			/* Icelandic Eth */
		'\u00a3',			/* UK pound symbol */
		'\u0153',			/* oe ligature */
		'\u0152',			/* OE ligature */
		'\u00a1',			/* inverse-! */
		'\u00bf',			/* inverse-? */
	};
	private static final String TAG = "ZScreen";

	public ZScreen(ZViewOutput[] twistyViews, Handler handler,
			String fixedFontFamily, String variableFontFamily, int font_size) {
		views = twistyViews;
		dialog_handler = handler;
		setFixedFont(fixedFontFamily, font_size);
		setVariableFont(fixedFontFamily, font_size);

		inputcodes = new SyncVector<Integer>();
		bufferedcodes = new Vector<Integer>();
		default_foreground = ZColor.Z_BLACK;
		default_background = ZColor.Z_WHITE;
	}

	public ZViewOutput getView(int index) {
		return views[index];
	}

	protected boolean isterminator(int key) {
		return ((key == 10) || (key == 13));
	}

	static char zascii_to_unicode(short zascii) {
		if ((zascii >= 32) && (zascii <= 126)) /* normal ascii */
		return (char)zascii;
		else if ((zascii >= 155) && (zascii <= 251)) {
			if ((zascii - 155) < accent_table.length) {
				return accent_table[zascii - 155];
			}
			else
				return '?';
		}
		else if ((zascii == 0) || (zascii >= 256)) {
			return '?';
		}
		else {
			Log.e(TAG, "Illegal character code: " + zascii);
			return '?';
		}
	}

	static short unicode_to_zascii(char unicode) throws NoSuchKeyException {
		short i;

		if (unicode == '\n')
			return 13;
		if (unicode == '\b')
			return 127;
		else if (((int)unicode < 0x20) &&
				(unicode != '\r' /*'\uu000d'*/) &&
				(unicode != '\uu001b'))
			throw new NoSuchKeyException("Illegal character input: " + (short)unicode);
		else if ((int)unicode < 0x80) /* normal ascii, including DELETE */
			return (short)unicode;
		else {
			for (i = 0; i < accent_table.length; i++) {
				if (accent_table[i] == unicode)
					return (short)(155 + i);
			}
			throw new NoSuchKeyException("Illegal character input: " + (short)unicode);
		}
	}

	static short fkey_to_zascii(int fkey) throws NoSuchKeyException {
		switch (fkey) {
		case Event.UP:		return 129;
		case Event.DOWN:	return 130;
		case Event.LEFT:	return 131;
		case Event.RIGHT:	return 132;
		case Event.F1:		return 133;
		case Event.F2:		return 134;
		case Event.F3:		return 135;
		case Event.F4:		return 136;
		case Event.F5:		return 137;
		case Event.F6:		return 138;
		case Event.F7:		return 139;
		case Event.F8:		return 140;
		case Event.F9:		return 141;
		case Event.F10:		return 142;
		case Event.F11:		return 143;
		case Event.F12:		return 144;
		default:
			throw new NoSuchKeyException("Illegal function key " + fkey);
		}
	}

	public boolean keyDown(Event e, int key) {

		short code;

		/* TODO: e, key to code */
		try {
			if (e.id == Event.KEY_PRESS)
				code = unicode_to_zascii((char)key);
			else /* if (e.action == Event.KEY_ACTION) */
				code = fkey_to_zascii(key);
			inputcodes.syncAddElement(new Integer(code));
		}
		catch (NoSuchKeyException excpt) {
			Log.w(TAG, "No such key in keyDown: " + key);
		}
		return true;
	}

	public void set_input_window(ZWindow thewindow)
	{
		inputwindow = thewindow;
	}

	public short read_code() {
		Integer thecode = null;

		while (thecode == null) {
			thecode = inputcodes.syncPopFirstElement();
		}
		return (short)thecode.intValue();
	}

	public short read_buffered_code() { /* should really be synched */
		Integer thecode;
		int incode;

		inputwindow.flush();

		while (!bufferdone) {
			inputwindow.flush();
			inputwindow.showCursor(true);
			incode = read_code();
			inputwindow.showCursor(false);
			if ((incode == 8) || (incode == 127)) {
				try {
					thecode = bufferedcodes.lastElement();
					bufferedcodes.removeElementAt(bufferedcodes.size() - 1);
					inputwindow.flush();
					inputwindow.backspace();
					inputwindow.flush();
				}
				catch (NoSuchElementException booga) {
					/* ignore */
				}
			}
			else {
				if (isterminator(incode)) {
					bufferdone = true;
					if ((incode==10) || (incode == 13))
						inputwindow.newline();
				}
				else {
					inputwindow.printzascii((short)incode);
					inputwindow.flush();
				}
				bufferedcodes.addElement(new Integer(incode));
			}
		}
		thecode = bufferedcodes.firstElement();
		bufferedcodes.removeElementAt(0);
		if (bufferedcodes.isEmpty()) {
			bufferdone = false;
		}
		return (short)(thecode.intValue());
	}

	/**
	 * Set the main font for the game.
	 * The Font Family can be any legal font name. However use of a
	 * non-fixed width font could cause many unexpected problems.
	 * Use at your own risk. Setting font_size to zero or below
	 * will set the size to DEFAULT_FONT_SIZE.
	 *
	 * @param font_family   a Font Family sting (i.e. "Courier").
	 * @param font_size     the point size of the font (int).
	 */ 
	public synchronized void setFixedFont( String font_family, int font_size )
	{
		this.fixedfont = new Font(font_family, Font.PLAIN, font_size);
	}

	public synchronized void setVariableFont( String font_family, int font_size )
	{
		this.variablefont = new Font(font_family, Font.PLAIN, font_size);
	}

	/**
	 * Get the main font for the game.
	 */
	public synchronized Font getFixedFont( )
	{
		return this.fixedfont;
	}

	public Handler getDialogHandler() {
		// return Twisty's main dialog handler
		return dialog_handler;
	}

	public int getZForeground() 
	{
		return default_foreground;
	}

	public int getZBackground() 
	{
		return default_background;
	}

	public void clearInputQueues() {
		bufferedcodes.removeAllElements();
		inputcodes.removeAllElements();
	}

	public void clear() {
		for (ZViewOutput v : views) {
			v.clear();
		}
	}

	/**
	 * Used for the ZMachine header only.
	 * @return some fictitious number
	 */
	public int getchars() {
		return 64;
	}

	/**
	 * Used for the ZMachine header only.
	 * @return some fictitious number
	 */
	public int getlines() {
		return 20;
	}

	public void onZmFinished(ZMachineException e) {
		for (ZViewOutput v : views) {
			v.onZmFinished(e);
		}
		views[0].tellOwnerZmFinished(e);
	}
}
