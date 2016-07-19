package com.hepai.test.seekTo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.hepai.vshop.Common.Library.Component.Fragment.BaseFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hepai.test.R;

/**
 * Created by Jarvis.
 */
public class IndexFragment extends BaseFragment {
    public static final String TAG = "com.hepai.test.seekTo.IndexFragment";


    private static final String%parKey="";

    private String%parVal;

    private OnFragmentInteractionListener mListener;

    public IndexFragment() {

    }

    public static IndexFragment newInstance(String%parVal) {
        IndexFragment fragment = new IndexFragment();
        Bundle args = new Bundle();
        args.putString( % parKey,%parVal);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            %parVal = getArguments().getString( % parKey);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_index_seekto, container, false);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onButtonPressed(String TAG) {
        if (mListener != null) {
            mListener.onFragmentInteraction(TAG);
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String TAG);
    }
}
