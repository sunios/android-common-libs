package telecom.ctbeacon.http;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AsyncHttp<T> extends AsyncTask<String, Integer, T>{
	private static final String TAG = "AsyncHttp";
	private static boolean debug = true;
	private static final String DEFAULT_CHARSET = "UTF-8";
	private Listener<T> listener;
	private static HttpClient client;
	private Object requestBody;
	private Method method = Method.GET;
	private Type type = new TypeToken<T>(){}.getType();
	
	public AsyncHttp(){
		if (client == null) {
			client = new DefaultHttpClient();
		}
	}
	
	@Override
	protected T doInBackground(String... params) {
		return doRequest(params[0]);
	}
	
	@Override
	protected void onPostExecute(T result) {
		if(listener != null){
			listener.onResult(result);
		}
	}
	
	@Override
	protected void onCancelled() {
		super.onCancelled();
		if(listener != null){
			listener.onCancel();
		}
	}
	
	private T doRequest(String url){
		if(method == Method.GET){
			return doGet(url);
		}else if(method == Method.POST){
			return doPost(url,requestBody);
		}
		return null;
	}
	
	public static enum Method{
		GET,POST
	}
	
	private T doGet(String url){
		HttpGet get = new HttpGet(url);
		return execute(get);
	}
	
	private T doPost(String url,Object requestBody) {
		HttpPost post = new HttpPost(url);
		if (requestBody != null) {
			String json = convertToJson(requestBody);
			log("AsyncHttp Request ==>"+json);
			post.addHeader("Content-Type", "application/json");
			StringEntity entity = null;
			try {
				entity = new StringEntity(json, DEFAULT_CHARSET);
			} catch (UnsupportedEncodingException e) {
				listener.onError(e.getMessage());
				e.printStackTrace();
			}
			if (entity != null) {
				post.setEntity(entity);
			}
		}
		return execute(post);
	}
	
	public void setRequestBody(Object requestBody){
		this.requestBody = requestBody;
	}
	
	private T execute(HttpUriRequest request) {
		try {
			HttpResponse response = client.execute(request);
			int code = response.getStatusLine().getStatusCode();
			if(code == HttpStatus.SC_OK){
				HttpEntity entity = response.getEntity();
				String result = EntityUtils.toString(entity, DEFAULT_CHARSET);
				return convertToObjcet(result);
			}
		} catch (Exception e) {
			listener.onError(e.getMessage());
			e.printStackTrace();
		} finally {
			if (request != null) {
				request.abort();
			}
		}
		return null;
	}
	
	public T convertToObjcet(String json) {
		return new Gson().fromJson(json,type);
	}

	private String convertToJson(Object object) {
		return new Gson().toJson(object);
	}
	
	public void execute(Method method,String url,Type type,Listener<T> listener){
		this.method = method;
		this.listener = listener;
		this.type = type;
		this.execute(url);
	}
	
	private void log(String msg){
		if(debug){
			Log.d(TAG, msg);
		}
	}
	
	public interface Listener<T>{
		void onResult(T result);
		void onError(String error);
		void onCancel();
	}
	
}
