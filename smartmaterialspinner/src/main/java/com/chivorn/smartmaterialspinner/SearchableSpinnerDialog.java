package com.chivorn.smartmaterialspinner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import com.chivorn.smartmaterialspinner.adapter.SearchAdapter;
import com.chivorn.smartmaterialspinner.util.StringUtils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SearchableSpinnerDialog<T> extends DialogFragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private static final String TAG = SearchableSpinnerDialog.class.getSimpleName();
    private static final String INSTANCE_LIST_ITEMS = "ListItems";
    private static final String INSTANCE_LISTENER_KEY = "OnSearchDialogEventListener";
    private static final String INSTANCE_SPINNER_KEY = "SmartMaterialSpinner";
    private ArrayAdapter<T> searchArrayAdapter;
    private ViewGroup searchHeaderView;
    private AppCompatTextView tvSearchHeader;
    private SearchView searchView;
    private TextView tvSearch;
    private ListView searchListView;
    private TextView tvListItem;
    private LinearLayout itemListContainer;
    public Button btnDismiss;

    private boolean isShowKeyboardOnStart;
    private boolean isEnableSearchHeader = true;
    private int headerBackgroundColor;
    private Drawable headerBackgroundDrawable;

    private int searchDropdownView;
    private int searchBackgroundColor;
    private Drawable searchBackgroundDrawable;
    private int searchHintColor;
    private int searchTextColor;
    private int searchFilterColor;

    private int searchListItemBackgroundColor;
    private Drawable searchListItemBackgroundDrawable;
    private int searchListItemColor;
    private int selectedSearchItemColor;
    private int selectedPosition = -1;
    private T selectedItem;

    private String searchHeaderText;
    private int searchHeaderTextColor;
    private String searchHint;
    private int searchDialogGravity = Gravity.TOP;

    private Typeface typeface;

    private boolean enableDismissSearch = false;
    private String dismissSearchText;
    private int dismissSearchColor;

    private OnSearchDialogEventListener onSearchDialogEventListener;
    private OnSearchTextChanged onSearchTextChanged;
    private DialogInterface.OnClickListener dialogListener;
    private SmartMaterialSpinner<T> smartMaterialSpinner;
    private boolean isDismissOnSelected = true;

    private List<T> items; // 存储所有数据项
    private List<T> allItemsFirst;
    private List<T> filteredItems; // 存储过滤后的数据项
    private final ConcurrentHashMap<String, TrieNode> pinyinTrieMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TrieNode> pinyinLetterTrieMap = new ConcurrentHashMap<>();

    public SearchableSpinnerDialog() {
    }

    public static SearchableSpinnerDialog newInstance(SmartMaterialSpinner smartMaterialSpinner, List items) {
        SearchableSpinnerDialog searchableSpinnerDialog = new SearchableSpinnerDialog();
        Bundle args = new Bundle();
        args.putSerializable(INSTANCE_LIST_ITEMS, (Serializable) items);
        args.putSerializable(INSTANCE_SPINNER_KEY, smartMaterialSpinner);
        searchableSpinnerDialog.setArguments(args);
        return searchableSpinnerDialog;
    }

    @Override
    @Deprecated
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState = setSavedInstanceState(outState);
        outState.putSerializable(INSTANCE_LISTENER_KEY, outState.getSerializable(INSTANCE_LISTENER_KEY));
        outState.putSerializable(INSTANCE_SPINNER_KEY, outState.getSerializable(INSTANCE_SPINNER_KEY));
        outState.putSerializable(INSTANCE_LIST_ITEMS, outState.getSerializable(INSTANCE_LIST_ITEMS));
        super.onSaveInstanceState(outState);
    }

    private Bundle setSavedInstanceState(Bundle savedInstanceState) {
        Bundle dialogInstanceState = this.getArguments();
        if (savedInstanceState == null || savedInstanceState.isEmpty() && dialogInstanceState != null) {
            savedInstanceState = dialogInstanceState;
        }
        return savedInstanceState;
    }

    @Override
    @SuppressWarnings({
            "unchecked",
            "rawtypes"
    })
    @Deprecated
    public void onCreate(Bundle savedInstanceState) {
        savedInstanceState = setSavedInstanceState(savedInstanceState);
        this.smartMaterialSpinner = (SmartMaterialSpinner) savedInstanceState.get(INSTANCE_SPINNER_KEY);
        this.onSearchDialogEventListener = smartMaterialSpinner;
        savedInstanceState.putSerializable(INSTANCE_LISTENER_KEY, onSearchDialogEventListener);
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    @Deprecated
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        savedInstanceState = setSavedInstanceState(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        if (savedInstanceState != null) {
            onSearchDialogEventListener = (OnSearchDialogEventListener) savedInstanceState.getSerializable(INSTANCE_LISTENER_KEY);
        }
        View searchLayout = inflater.inflate(R.layout.smart_material_spinner_searchable_dialog_layout, null);
        initSearchDialog(searchLayout, savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(searchLayout);

        AlertDialog dialog = builder.create();
        setGravity(dialog);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        savedInstanceState = setSavedInstanceState(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @SuppressWarnings({
            "unchecked",
            "rawtypes"
    })
    @Deprecated
    private void initSearchDialog(View rootView, Bundle savedInstanceState) {
        searchHeaderView = rootView.findViewById(R.id.search_header_layout);
        tvSearchHeader = rootView.findViewById(R.id.tv_search_header);
        searchView = rootView.findViewById(R.id.search_view);
        tvSearch = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchListView = rootView.findViewById(R.id.search_list_item);
        itemListContainer = rootView.findViewById(R.id.item_search_list_container);
        btnDismiss = rootView.findViewById(R.id.btn_dismiss);

        if (getActivity() != null) {
            SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            if (searchManager != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            }
        }
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
        if (isShowKeyboardOnStart) {
            searchView.requestFocus();
        } else {
            searchView.clearFocus();
        }

        List items = savedInstanceState != null ? (List) savedInstanceState.getSerializable(INSTANCE_LIST_ITEMS) : null;
        if (items != null) {
            this.items = items; // 保存这个是实时的 所有数据项
            this.allItemsFirst = new ArrayList<>(this.items);  // 保存所有数据项
            // 初始化拼音和首字母索引
            initPinyinAndLetterIndex();
            searchArrayAdapter = new SearchAdapter<T>(getActivity(), searchDropdownView, items) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View listView = super.getView(position, convertView, parent);
                    tvListItem = (TextView) listView;
                    tvListItem.setTypeface(typeface);
                    SpannableString spannableString = new SpannableString(tvListItem.getText());
                    if (searchListItemBackgroundColor != 0) {
                        itemListContainer.setBackgroundColor(searchListItemBackgroundColor);
                    } else if (searchListItemBackgroundDrawable != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            itemListContainer.setBackground(searchListItemBackgroundDrawable);
                        }
                    }

                    if (searchListItemColor != 0) {
                        tvListItem.setTextColor(searchListItemColor);
                        if (searchFilterColor != 0 && searchView.getQuery() != null && !searchView.getQuery().toString().isEmpty()) {
                            String query = StringUtils.removeDiacriticalMarks(searchView.getQuery().toString()).toLowerCase(Locale.getDefault());
                            String fullText = StringUtils.removeDiacriticalMarks(tvListItem.getText().toString()).toLowerCase(Locale.getDefault());
                            if (!TextUtils.isEmpty(query)) {
                                int start = fullText.indexOf(query);
                                int end = start + query.length();
                                if (start >= 0 && end >= 0) {
                                    spannableString.setSpan(new ForegroundColorSpan(searchFilterColor), start, end, 0);
                                }
                            }
                            tvListItem.setText(spannableString, TextView.BufferType.SPANNABLE);
                        }
                    }

                    T item = searchArrayAdapter.getItem(position);
                    if (selectedSearchItemColor != 0 && position >= 0 && item != null && item.equals(selectedItem)) {
                        tvListItem.setTextColor(selectedSearchItemColor);
                    }
                    return listView;
                }
            };
        }
        searchListView.setAdapter(searchArrayAdapter);
        searchListView.setTextFilterEnabled(true);
        searchListView.setOnItemClickListener((parent, view, position, id) -> {
            if (onSearchDialogEventListener != null) {
                onSearchDialogEventListener.onSearchItemSelected(searchArrayAdapter.getItem(position), position);
                selectedItem = searchArrayAdapter.getItem(position);
            }
            getDialog().dismiss();
        });

        searchListView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                scrollToSelectedItem();
            } else if (bottom > oldBottom) {
                scrollToSelectedItem();
            }
        });

        btnDismiss.setOnClickListener(v -> dismiss());

        initSearchHeader();
        initSearchBody();
        initSearchFooter();
    }

    private void initSearchHeader() {
        if (isEnableSearchHeader) {
            searchHeaderView.setVisibility(View.VISIBLE);
        } else {
            searchHeaderView.setVisibility(View.GONE);
        }

        if (searchHeaderText != null) {
            tvSearchHeader.setText(searchHeaderText);
            tvSearchHeader.setTypeface(typeface);
        }

        if (searchHeaderTextColor != 0) {
            tvSearchHeader.setTextColor(searchHeaderTextColor);
        }

        if (headerBackgroundColor != 0) {
            searchHeaderView.setBackgroundColor(headerBackgroundColor);
        } else if (headerBackgroundDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                searchHeaderView.setBackground(headerBackgroundDrawable);
            }
        }
    }

    private void initSearchBody() {
        if (searchHint != null) {
            searchView.setQueryHint(searchHint);
        }
        if (searchBackgroundColor != 0) {
            searchView.setBackgroundColor(searchBackgroundColor);
        } else if (searchBackgroundDrawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                searchView.setBackground(searchBackgroundDrawable);
            }
        }
        if (tvSearch != null) {
            tvSearch.setTypeface(typeface);
            if (searchTextColor != 0) {
                tvSearch.setTextColor(searchTextColor);
            }
            if (searchHintColor != 0) {
                tvSearch.setHintTextColor(searchHintColor);
            }
        }
    }

    private void initSearchFooter() {
        if (enableDismissSearch)
            btnDismiss.setVisibility(View.VISIBLE);
        if (dismissSearchText != null)
            btnDismiss.setText(dismissSearchText);
        if (dismissSearchColor != 0)
            btnDismiss.setTextColor(dismissSearchColor);
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (onSearchDialogEventListener != null) {
            onSearchDialogEventListener.onSearchableSpinnerDismiss();
        }
        super.onDismiss(dialog);
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String searchText) {
        // 判断搜索内容是否为空
        if (TextUtils.isEmpty(searchText)) {
            // 显示所有数据
            filteredItems = allItemsFirst;
        } else {
            // 创建一个新的列表用于存储匹配的数据
            filteredItems = new ArrayList<>();

            searchText = searchText.toLowerCase(); // 统一为小写
            searchTextPaired(searchText);
        }
        // 刷新列表数据
        // 1. 使用 LinkedHashSet 去重并保持顺序
        Set<T> uniqueItems = new LinkedHashSet<>(filteredItems);
        // 2. 将去重后的结果重新添加到 filteredItems 列表中
        filteredItems.clear(); // 清空原有的 filteredItems 列表
        filteredItems.addAll(uniqueItems);

        searchArrayAdapter.clear();
        searchArrayAdapter.addAll(filteredItems);
        searchArrayAdapter.notifyDataSetChanged();
        return true;
    }

    // 建立拼音前缀索引
    // Trie 树节点
    class TrieNode {
        char value;
        Map<Character, TrieNode> children;
        List<T> data;

        TrieNode(char value) {
            this.value = value;
            this.children = new HashMap<>();
            this.data = new ArrayList<>();
        }
    }

    // Trie 树
    class Trie {
        TrieNode root;

        Trie() {
            root = new TrieNode(' '); // 根节点
        }

        // 插入拼音或首字母
        void insert(String key, T data) {
            TrieNode node = root;
            for (char c : key.toCharArray()) {
                if (!node.children.containsKey(c)) {
                    node.children.put(c, new TrieNode(c));
                }
                node = node.children.get(c);
            }
            node.data.add(data);
        }

        // 查询包含 searchText 的所有数据项
        List<T> search(String searchText) {
            TrieNode node = root;
            for (char c : searchText.toCharArray()) {
                if (!node.children.containsKey(c)) {
                    return new ArrayList<>(); // 没有匹配项
                }
                node = node.children.get(c);
            }
            return node.data; // 返回所有匹配项
        }
    }

    // 初始化拼音 index
    private void initPinyinAndLetterIndex() {
        HanyuPinyinOutputFormat outputFormat = new HanyuPinyinOutputFormat();
        outputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        outputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);

        for (T item : allItemsFirst) {
            String itemStr = item.toString().toLowerCase();

            if (isChinese(itemStr.charAt(0))) {
                String pinyin = convertToPinyin(itemStr);
                String pinyinLetter = convertToPinyinLetter(itemStr);

                // 使用 Trie 树插入拼音和首字母，并存储原始中文值
                // 插入完整的拼音或首字母
                insertTrieNode(pinyinTrieMap, pinyin, item); // 传递原始中文值
                insertTrieNode(pinyinLetterTrieMap, pinyinLetter, item); // 传递原始中文值
                // 插入所有前缀
                for (int i = 1; i < pinyin.length(); i++) {
                    String prefix = pinyin.substring(0, i);
                    insertTrieNode(pinyinTrieMap, prefix, item);
                }
                for (int i = 1; i < pinyinLetter.length(); i++) {
                    String prefix = pinyinLetter.substring(0, i);
                    insertTrieNode(pinyinLetterTrieMap, prefix, item);
                }
            }
        }
    }

    private void insertTrieNode(ConcurrentHashMap<String, TrieNode> trieMap, String key, T originalValue) {
        TrieNode node = trieMap.get(key);
        if (node == null) {
            node = new TrieNode(' '); // 创建新的节点
            trieMap.put(key, node);
        }

        // 将原始中文值存储在节点的 data 列表中
        node.data.add(originalValue);

        // 将 key 的所有前缀也插入到 Trie 树中
        for (int i = 1; i <= key.length(); i++) {
            String prefix = key.substring(0, i);
            if (!trieMap.containsKey(prefix)) {
                trieMap.put(prefix, new TrieNode(' '));
            }
        }
    }


    private void searchTextPaired(String searchText) {
        if (isAllNonChinese(searchText)) {
            // 所有字符都不包含中文
            // 非中文匹配
            for (T item : allItemsFirst) {
                String itemStr = item.toString().toLowerCase();
                if (!isChinese(itemStr.charAt(0)) && itemStr.contains(searchText)) {
                    filteredItems.add(item);
                }
            }

            // 使用 Trie 树进行包含查询
            // 拼音匹配
            List<T> matchedPinyinItems = search(searchText, pinyinTrieMap);
            if (!matchedPinyinItems.isEmpty()) {
                filteredItems.addAll(matchedPinyinItems);
            }
            // 拼音首字母匹配
            List<T> matchedLetterItems = search(searchText, pinyinLetterTrieMap);
            if (!matchedLetterItems.isEmpty()) {
                filteredItems.addAll(matchedLetterItems);
            }
        } else {
            // 包含中文
            // 汉字匹配
            for (T item : allItemsFirst) {
                String itemStr = item.toString().toLowerCase();
                if (itemStr.contains(searchText)) {
                    filteredItems.add(item);
                    return;
                }
            }
        }
    }

    // 查询包含 searchText 的所有数据项
    List<T> search(String searchText, ConcurrentHashMap<String, TrieNode> trieMap) {
        TrieNode node = trieMap.get(searchText);
        if (node == null) {
            return new ArrayList<>(); // 没有匹配项
        }
        // 使用 Set 存储搜索结果，避免重复元素
        Set<T> result = new HashSet<>(node.data);
        return new ArrayList<>(result); // 返回去重后的结果
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
            return true;
        }
        return false;
    }

    private boolean isAllNonChinese(String text) {
        for (char c : text.toCharArray()) {
            if (isChinese(c)) {
                return false; // 存在中文，返回false
            }
        }
        return true; // 所有字符都为非中文，返回true
    }

    public String convertToPinyin(String chineseText) {
        StringBuilder pinyin = new StringBuilder();
        for (char c : chineseText.toCharArray()) {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
            if (pinyinArray != null) {
                // 使用正则表达式去除声调数字
                String pinyinWithoutTone = pinyinArray[0].replaceAll("\\d", "");
                pinyin.append(pinyinWithoutTone);
            } else {
                Log.e(TAG, "拼音转换错误：" + c);
                // 如果无法转换，则保留原字符
            }
        }
        return pinyin.toString();
    }

    public String convertToPinyinLetter(String chineseText) {
        StringBuilder pinyinLetter = new StringBuilder();
        for (char c : chineseText.toCharArray()) {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
            if (pinyinArray != null) {
                char charLatter = pinyinArray[0].toString().charAt(0);
                pinyinLetter.append(charLatter); // 取第一个拼音
            } else {
                Log.e(TAG, "拼音转换错误：" + c);
                // 如果无法转换，则保留原字符
            }
        }
        return pinyinLetter.toString();
    }

    @Override
    public boolean onClose() {
        return false;
    }

    public interface OnSearchDialogEventListener<T> extends Serializable {
        void onSearchItemSelected(T item, int position);

        void onSearchableSpinnerDismiss();
    }

    public interface OnSearchTextChanged {
        void onSearchTextChanged(String strText);
    }

    public void setOnSearchDialogEventListener(OnSearchDialogEventListener onSearchDialogEventListener) {
        this.onSearchDialogEventListener = onSearchDialogEventListener;
    }

    public void setOnSearchTextChangedListener(OnSearchTextChanged onSearchTextChanged) {
        this.onSearchTextChanged = onSearchTextChanged;
    }

    public void setEnableSearchHeader(boolean enableSearchHeader) {
        isEnableSearchHeader = enableSearchHeader;
    }

    public void setShowKeyboardOnStart(boolean showKeyboardOnStart) {
        isShowKeyboardOnStart = showKeyboardOnStart;
    }

    public void setSearchHeaderText(String header) {
        searchHeaderText = header;
    }

    public void setSearchHeaderTextColor(int color) {
        this.searchHeaderTextColor = color;
    }

    public void setSearchHeaderBackgroundColor(int color) {
        headerBackgroundColor = color;
        headerBackgroundDrawable = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setSearchHeaderBackgroundColor(Drawable drawable) {
        headerBackgroundDrawable = drawable;
        headerBackgroundColor = 0;
    }

    public int getSearchDropdownView() {
        return searchDropdownView;
    }

    public void setSearchDropdownView(int searchDropdownView) {
        this.searchDropdownView = searchDropdownView;
    }

    public void setSearchBackgroundColor(int color) {
        searchBackgroundColor = color;
        searchBackgroundDrawable = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setSearchBackgroundColor(Drawable drawable) {
        searchBackgroundDrawable = drawable;
        searchBackgroundColor = 0;
    }


    public void setSearchListItemBackgroundColor(int color) {
        searchListItemBackgroundColor = color;
        searchListItemBackgroundDrawable = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setSearchListItemBackgroundDrawable(Drawable drawable) {
        searchListItemBackgroundDrawable = drawable;
        searchListItemBackgroundColor = 0;
    }

    public void setSearchHint(String searchHint) {
        this.searchHint = searchHint;
    }

    public void setSearchTextColor(int color) {
        searchTextColor = color;
    }

    public void setSearchFilterColor(int searchFilterColor) {
        this.searchFilterColor = searchFilterColor;
    }

    public void setSearchHintColor(int color) {
        searchHintColor = color;
    }

    public void setSearchListItemColor(int searchListItemColor) {
        this.searchListItemColor = searchListItemColor;
    }

    public void setSelectedSearchItemColor(int selectedSearchItemColor) {
        this.selectedSearchItemColor = selectedSearchItemColor;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }

    public void setGravity(int gravity) {
        this.searchDialogGravity = gravity;
    }

    private void setGravity(Dialog dialog) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setGravity(searchDialogGravity);
        }
    }

    private void scrollToSelectedItem() {
        if (selectedPosition >= 0 && searchListView.isSmoothScrollbarEnabled()) {
            searchListView.smoothScrollToPositionFromTop(selectedPosition, 0, 10);
        }
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public boolean isEnableDismissSearch() {
        return enableDismissSearch;
    }

    public void setEnableDismissSearch(boolean enableDismissSearch) {
        this.enableDismissSearch = enableDismissSearch;
    }

    public String getDismissSearchText() {
        return dismissSearchText;
    }

    public void setDismissSearchText(String dismissSearchText) {
        this.dismissSearchText = dismissSearchText;
    }

    public int getDismissSearchColor() {
        return dismissSearchColor;
    }

    public void setDismissSearchColor(int dismissSearchColor) {
        this.dismissSearchColor = dismissSearchColor;
    }
}
