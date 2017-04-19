package com.spaculus.americanbars.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.spaculus.adapters.CustomMyAlbumsAdapter;
import com.spaculus.americanbars.BaseActivity;
import com.spaculus.americanbars.R;
import com.spaculus.americanbars.dialogfragments.FragmentAlertDialogDelete;
import com.spaculus.beans.Album;
import com.spaculus.helpers.ConfigConstants;
import com.spaculus.helpers.LoadMoreListView;
import com.spaculus.helpers.LoadMoreListView.OnLoadMoreListener;
import com.spaculus.helpers.ServiceHandler;
import com.spaculus.helpers.SessionManager;
import com.spaculus.helpers.Utils;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FragmentMyAlbums extends Fragment {
	
	private View rootView = null;
	private LoadMoreListView listViewMyAlbums;
	private EditText etSearch;
	private ImageView ivSearch, ivRefresh;
	private Button buttonSelectAll, buttonDelete, buttonAddAlbum;

	//  For the Bar List
	private ArrayList<Album> myAlbumsList = null;
 	private CustomMyAlbumsAdapter adapter = null;
	
	//  For the Web service response
	//  Implement Paging functionality to get the people list
	//  current_page = page_number = offset(in web service method)
	private int offset = 0; 
	private int totalRecords = 0;
	private boolean isLoadMoreButtonClicked = false;
	
	//  To know whether user is redirected to this screen from navigation drawer or not
	//  So on the basis of it we can show the action bar fields
	boolean isRedirectedFromMainActivity = false;
	
	public FragmentMyAlbums(boolean flag){
		this.isRedirectedFromMainActivity = flag;
	}
	private RelativeLayout relativeLayout;
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
			rootView = inflater.inflate(R.layout.fragment_my_albums, container, false);
			
			//  For the Action Bar
			//boolean isBackArrowVisible, boolean isTitleVisible, boolean isLogoVisible, boolean isMenuIconVisible
			if(isRedirectedFromMainActivity) {
				((BaseActivity)getActivity()).setActionBarFromChild(false, true, false, true, true);	
			}
			else {
				((BaseActivity)getActivity()).setActionBarFromChild(true, true, false, true, true);
			}
			
			//  Mapping of all the views
			mappedAllViews();
			relativeLayout.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					hideSoftKeyboard();
				}
			});
			//  Initialize adapter and array list here.
			myAlbumsList = new ArrayList<Album>();
			adapter = new CustomMyAlbumsAdapter(getActivity(), R.layout.fragment_my_albums_list_item, myAlbumsList, FragmentMyAlbums.this);
			listViewMyAlbums.setAdapter(adapter);
			
			//  Now, get the search My Favorite Bars List here
			loadData();
			
			//  Set a listener to be invoked when the list reaches the end
			listViewMyAlbums.setOnLoadMoreListener(new OnLoadMoreListener() {
				public void onLoadMore() {
					try {
						if (totalRecords != 0) {
							isLoadMoreButtonClicked = true;
							if (offset < totalRecords && totalRecords>ConfigConstants.Constants.limit) {
								offset = myAlbumsList.size();
								//  Now, get the search My Favorite Bars List here
								if(Utils.getInstance() .isNetworkAvailable(getActivity())) {
									//Log.i("Load More Call", "Load More Call");
									new AsynTaskGetMyAlbumsList().execute();
								}
								else{
									Utils.getInstance().showToastNoInternetAvailable(getActivity());
								}
							}
							else {
								//  A method to called when no data is available
								noDataAvailable();
							}
						}
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
				
			//  Search icon click event
			ivSearch.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					performSearch();
				}
			});
				
			//  Refresh icon click event
			ivRefresh.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						//  Now, here first of all hide the soft keyboard
						hideSoftKeyboard();
					
						//  Clear the Search Text
						etSearch.setText("");
						//  Now, here initialize here offset again as for this we have to load initial data only
						loadData();
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
				
			//  ListView row click event
			listViewMyAlbums.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					try {
						viewAlbumDetails(myAlbumsList.get(position).getStatus(), myAlbumsList.get(position).getBar_gallery_id(), myAlbumsList.get(position).getTitle());
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			//  Select All button click event
			buttonSelectAll.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					try {
						/* Now, first of all check whether data is there or not. */
						if(myAlbumsList.size()>0) {
							//  Now, first of all check whether need to select or deselect all the checkboxes
							//  Means need to Select All
							if(buttonSelectAll.getText().equals(getResources().getString(R.string.btn_select_all))) {
								selectDeselectAllCheckBoxes(true);
								buttonSelectAll.setText(getResources().getString(R.string.btn_deselect_all));
							}
							else {
								selectDeselectAllCheckBoxes(false);
								buttonSelectAll.setText(getResources().getString(R.string.btn_select_all));
							}
						}
						else {
							//Toast.makeText(getActivity(), ConfigConstants.getInstance().noMyAlbum, Toast.LENGTH_SHORT).show();
							Utils.toastLong(getActivity(), ConfigConstants.Messages.noMyAlbum);
						}
					} 
					catch (NotFoundException e) {
						e.printStackTrace();
					}
				}
			});
				
			//  Delete button click event
			buttonDelete.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					try {
						/* Now, first of all check whether data is there or not. */
						if(myAlbumsList.size()>0) {
							//  Generate a string for the id
							String generatedString = "";
							for(int i=0; i<myAlbumsList.size(); i++) {
								if(myAlbumsList.get(i).isSelected()) {
									if(generatedString.isEmpty()) {
										generatedString = myAlbumsList.get(i).getBar_gallery_id();
									}
									else {
										generatedString += ","+myAlbumsList.get(i).getBar_gallery_id();
									}
								}
							}
							
							//  To delete multiple bars
							if(generatedString.isEmpty()) {
								//  Means no record is selected to delete.
								//Toast.makeText(getActivity(), ConfigConstants.getInstance().selectRecordToDelete, Toast.LENGTH_SHORT).show();
								Utils.toastLong(getActivity(), ConfigConstants.Messages.selectRecordToDelete);
							}
							else {
								showAlertDialogMethod(generatedString);	
							}
						}
						else {
							//Toast.makeText(getActivity(), ConfigConstants.getInstance().noMyAlbum, Toast.LENGTH_SHORT).show();
							Utils.toastLong(getActivity(), ConfigConstants.Messages.noMyAlbum);
						}
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			//  Add Album button click event
			buttonAddAlbum.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						((BaseActivity) getActivity()).redirectAddAlbumActivity(getActivity(), "");
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			    @Override
			    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
			            performSearch();
			            return true;
			        }
			        return false;
			    }
			});
		} 
        catch (Exception e) {
			e.printStackTrace();
		}
        return rootView;
    }
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		/*
		 * This need to be implement as if user goes to the details screen and
		 * done remove favorite from that screen.
		 */
		/*if(ConfigConstants.isAddToFavoriteORLikeBarButtonClicked || ConfigConstants.isAddEditAlbum) {
			ConfigConstants.isAddToFavoriteORLikeBarButtonClicked = false;
			ConfigConstants.isAddEditAlbum = false;
			//  Now, get the search My Favorite Bars List here
			loadData();
		}*/
		if(ConfigConstants.isAddEditAlbum) {
			ConfigConstants.isAddEditAlbum = false;
			//  Now, get the search My Favorite Bars List here
			loadData();
		}
	}
	
	//  This method is used to do the mapping of all the views.
	private void mappedAllViews() {
		try {
			listViewMyAlbums = (LoadMoreListView) rootView.findViewById(R.id.listViewMyAlbums);
			etSearch = (EditText) rootView.findViewById(R.id.etSearch);
			ivSearch = (ImageView) rootView.findViewById(R.id.ivSearch);
			ivRefresh = (ImageView) rootView.findViewById(R.id.ivRefresh);
			buttonSelectAll = (Button) rootView.findViewById(R.id.btnSelectAll);
			buttonDelete = (Button) rootView.findViewById(R.id.btnDelete);
			buttonAddAlbum = (Button) rootView.findViewById(R.id.btnAddAlbum);
			relativeLayout = (RelativeLayout) rootView.findViewById(R.id.mainRele);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//  AsyncTask to get the Albums List based on the entered criteria
	public class AsynTaskGetMyAlbumsList extends AsyncTask<Void, Void, Void> {
		
		private ProgressDialog pd = new ProgressDialog(getActivity());
    	private String responseString = "";
    	private String status = "";
    	
    	@Override
		protected void onPreExecute() {
			try {
				super.onPreExecute();
				
				this.pd.setMessage(ConfigConstants.Messages.loadingMessage);
				pd.setCanceledOnTouchOutside(false); 
				pd.setCancelable(false);
				this.pd.show();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}	

		@Override
		protected Void doInBackground(Void... params) {
	        try {
	        	ServiceHandler sh = new ServiceHandler();
	        	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        	nameValuePairs.add(new BasicNameValuePair("user_id", SessionManager.getInstance(getActivity()).getData(SessionManager.KEY_USER_ID)));
		        nameValuePairs.add(new BasicNameValuePair("device_id", SessionManager.getInstance(getActivity()).getData(SessionManager.KEY_DEVICE_ID)));
		        nameValuePairs.add(new BasicNameValuePair("unique_code", SessionManager.getInstance(getActivity()).getData(SessionManager.KEY_UNIQUE_CODE)));
		        nameValuePairs.add(new BasicNameValuePair("limit", ""+ConfigConstants.Constants.limit));
		        nameValuePairs.add(new BasicNameValuePair("offset", ""+offset));
		        nameValuePairs.add(new BasicNameValuePair("keyword", etSearch.getText().toString().trim()));
		        
	        	// Making a request to URL and getting response
				responseString = sh.makeServiceCall(ConfigConstants.Urls.get_user_album, ServiceHandler.POST, nameValuePairs);
	        	//Log.i("responseString",responseString);
	        }
	        catch(Exception e) {
	        	e.printStackTrace();
	        }
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			try {
				super.onPostExecute(result);
				listViewMyAlbums.onLoadMoreComplete();
				JSONObject jsonObjParent = null;
				if(responseString!=null){
					jsonObjParent = new JSONObject(responseString);
					status = jsonObjParent.getString("status");
					//Log.i("status",status);
				}
				
				if(status.equals(ConfigConstants.Messages.RESPONSE_SUCCESS)){
					
					totalRecords = jsonObjParent.getInt("album_total");
					
					//  Now, check whether data exists or not
					if(totalRecords>0) {
						if (!isLoadMoreButtonClicked) {
							myAlbumsList.clear();
						}
						
						JSONObject jsonObjectmyFavoriteBarsList = jsonObjParent.getJSONObject("album");
						JSONArray jsonArrayResult = jsonObjectmyFavoriteBarsList.getJSONArray("result");
						
						for (int i = 0; i < jsonArrayResult.length(); i++) {
							JSONObject jsonObj = jsonArrayResult.getJSONObject(i);
							myAlbumsList.add(new Album(jsonObj.getString("bar_gallery_id"), jsonObj.getString("bar_id"), jsonObj.getString("title"), jsonObj.getString("description"), 
									jsonObj.getString("date_added"), jsonObj.getString("gallery"), jsonObj.getString("status"), jsonObj.getString("reorder"), false));
						}
						
						//  Make ListView visible here.
						listViewMyAlbums.setVisibility(View.VISIBLE);
						//  notifying list adapter about data changes so that it renders the list view with updated data
						adapter.notifyDataSetChanged();
						
						// Call onLoadMoreComplete when the LoadMore task, has finished
						((LoadMoreListView) listViewMyAlbums).onLoadMoreComplete();
					}
					else{
						//  A method to called when no data is available
						noDataAvailable();
					}
				}
				else {
					//  A method to called when no data is available
					noDataAvailable();}
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
			if(this.pd.isShowing()) {
				this.pd.dismiss();
			}
		}	
		
		@Override
		protected void onCancelled() {
			try {
				// Notify the loading more operation has finished
				((LoadMoreListView) listViewMyAlbums).onLoadMoreComplete();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
	
	//  Hide the soft key board on the click of Layout
	private void hideSoftKeyboard(){
		try {
			//Log.i("Method hideSoftKeyboard","Method hideSoftKeyboard");
			InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//  A method to called when no data is available
	private void noDataAvailable() {
		try {
			if (isLoadMoreButtonClicked) {
				isLoadMoreButtonClicked = false;
				listViewMyAlbums.hideFooterText();
				//Toast.makeText(getActivity(), ConfigConstants.getInstance().noMoreRecordFound, Toast.LENGTH_SHORT).show();
				Utils.showToastLong(getActivity(), ConfigConstants.Messages.noMoreRecordFound);
			} 
			else {
				listViewMyAlbums.hideFooterText();
				listViewMyAlbums.setVisibility(View.GONE);
				//Toast.makeText(getActivity(), ConfigConstants.getInstance().noRecordFound, Toast.LENGTH_SHORT).show();
				Utils.showToastLong(getActivity(), ConfigConstants.Messages.noRecordFound);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* A method is used to select or deselect all the CheckBoxes. */
	private void selectDeselectAllCheckBoxes(boolean status) {
		try {
			for(int i=0; i<myAlbumsList.size(); i++) {
				myAlbumsList.get(i).setSelected(status);
			}
			//  Now, notify an adapter
			adapter.notifyDataSetChanged();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* A method is used to change the status of the "Select All" button text. */ 
	public void checkButtonText() {
		try {
			boolean isAllCheckBoxesChecked = true;
			for(int i=0; i<myAlbumsList.size(); i++) {
				if(!myAlbumsList.get(i).isSelected()) {
					isAllCheckBoxesChecked = false;
					break;
				}
			}
			//  Now, set the button text
			if(isAllCheckBoxesChecked) {
				buttonSelectAll.setText(getResources().getString(R.string.btn_deselect_all));
			}
			else {
				buttonSelectAll.setText(getResources().getString(R.string.btn_select_all));
			}
		} 
		catch (NotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/* To show an alert dialog */
	public void showAlertDialogMethod(String id) {
	
		try {
			//  For the Delete functionality
			FragmentAlertDialogDelete objCreateDelete = new FragmentAlertDialogDelete("Delete Album", "Are you sure you want to delete this album?", "OK", "Cancel", ConfigConstants.Constants.MY_ALBUMS, id, FragmentMyAlbums.this);
			objCreateDelete.show(getFragmentManager(), "dialog");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//  AsyncTask to delete an album
	public class AsyncTaskDeleteAlbum extends AsyncTask<String, Void, Void> {
		
		private ProgressDialog pd = new ProgressDialog(getActivity());
    	private String responseString = "";
    	private String status = "";
    	private String generatedIDString = "";
    	
    	@Override
		protected void onPreExecute() {
			try {
				super.onPreExecute();
				
				this.pd.setMessage(ConfigConstants.Messages.loadingMessage);
				pd.setCanceledOnTouchOutside(false); 
				pd.setCancelable(false);
				this.pd.show();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}	

		@Override
		protected Void doInBackground(String... params) {
	        try {
	        	ServiceHandler sh = new ServiceHandler();
				generatedIDString = params[0];
	        	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        	nameValuePairs.add(new BasicNameValuePair("user_id", SessionManager.getInstance(getActivity()).getData(SessionManager.KEY_USER_ID)));
		        nameValuePairs.add(new BasicNameValuePair("device_id", SessionManager.getInstance(getActivity()).getData(SessionManager.KEY_DEVICE_ID)));
		        nameValuePairs.add(new BasicNameValuePair("unique_code", SessionManager.getInstance(getActivity()).getData(SessionManager.KEY_UNIQUE_CODE)));
		        nameValuePairs.add(new BasicNameValuePair("bar_gallery_id", generatedIDString));
		        			
	        	// Making a request to URL and getting response
				responseString = sh.makeServiceCall(ConfigConstants.Urls.bargallerydelete, ServiceHandler.POST, nameValuePairs);
	        	//Log.i("responseString",responseString);
	        }
	        catch(Exception e) {
	        	e.printStackTrace();
	        }
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			try {
				super.onPostExecute(result);
				JSONObject jsonObjParent = null;
				if(responseString!=null){
					jsonObjParent = new JSONObject(responseString);
					status = jsonObjParent.getString("status");
					//Log.i("status",status);
				}
				
				if(status.equals(ConfigConstants.Messages.RESPONSE_SUCCESS)) {
					//  Now, generate the IDs list
					List<String> idList = Arrays.asList(generatedIDString.split("\\s*,\\s*"));
					for(int i=0; i<myAlbumsList.size(); i++) {
						for(int j=0; j<idList.size(); j++) {
							if(myAlbumsList.get(i).getBar_gallery_id().equals(idList.get(j))) {
								myAlbumsList.remove(i);
							}
						}
					}
					//  Now, notify the adapter
					adapter.notifyDataSetChanged();
				}
				else {
					//Toast.makeText(getActivity(), "Favorite Bar is not deleted. Please try again.", Toast.LENGTH_SHORT).show();
					Utils.toastLong(getActivity(), "Favorite Bar is not deleted. Please try again.");
				}
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
			if(this.pd.isShowing()) {
				this.pd.dismiss();
			}
		}
	}
	
	//  A method to load the data initially
	private void loadData() {
		try {
			//  Now, here initialize here offset again as for this we have to load initial data only
			offset = 0;
			isLoadMoreButtonClicked = false;
			//  Now, get the search Bar List here
			if(Utils.getInstance().isNetworkAvailable(getActivity())) {
				new AsynTaskGetMyAlbumsList().execute();
			}
			else{
				Utils.getInstance().showToastNoInternetAvailable(getActivity());
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* For the My Albums Screen */
	public void editAlbum(String bar_gallery_id) {
		try {
			((BaseActivity) getActivity()).redirectAddAlbumActivity(getActivity(), bar_gallery_id);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* A method is used to view the album details screen. */
	public void viewAlbumDetails(String status, String bar_gallery_id, String title) {
		//  Now, first of all check the status of the album
		/*if(status.equals("Active")) {*/
			try {
				((BaseActivity) getActivity()).redirectPhotoGalleryDetailsActivity(getActivity(), bar_gallery_id, title, ConfigConstants.Urls.get_album_by_id);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		/*}
		else {
			//Toast.makeText(getActivity(), " Your selected album status is Inactive make it as Active to see images.", Toast.LENGTH_SHORT).show();
			Utils.toastLong(getActivity(), " Your selected album status is Inactive make it as Active to see images.");
		}*/
	}
	
	/* A method is used for the search functionality. */
	private void performSearch() {
		try {
			//  Now, here first of all hide the soft keyboard
			hideSoftKeyboard();
			
			//  Now, here initialize here offset again as for this we have to load initial data only
			loadData();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
