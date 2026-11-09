package com.github.mizo432.reg.ui;

import java.util.ResourceBundle;

/**
 * メッセージリソースバンドルへのアクセスを提供するユーティリティクラス。
 * UTF-8エンコーディングのpropertiesファイルから日本語メッセージを取得します。
 */
public class MessageSource {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages", java.util.Locale.JAPAN);

    /**
     * キーに対応するメッセージを取得します。
     * キーが見つからない場合はキー自体を返します。
     *
     * @param key メッセージキー
     * @return メッセージ文字列（キーが見つからない場合はキー自体）
     */
    public static String getMessage(String key) {
        try {
            return BUNDLE.getString(key);
        } catch (java.util.MissingResourceException e) {
            return "[" + key + "]";
        }
    }

    /**
     * 引数を埋め込んだメッセージを取得します。
     * キーが見つからない場合はキー自体を返します。
     *
     * @param key メッセージキー
     * @param args 埋め込み引数
     * @return 埋め込み済みのメッセージ文字列（キーが見つからない場合はキー自体）
     */
    public static String getMessage(String key, Object... args) {
        try {
            String template = BUNDLE.getString(key);
            return java.text.MessageFormat.format(template, args);
        } catch (java.util.MissingResourceException e) {
            return "[" + key + "]";
        }
    }
}
