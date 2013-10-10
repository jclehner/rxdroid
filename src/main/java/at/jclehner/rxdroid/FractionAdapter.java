package at.jclehner.rxdroid;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class FractionAdapter extends TypeAdapter<Fraction>
{
	@Override
	public Fraction read(JsonReader jsonReader) throws IOException {
		return Fraction.valueOf(jsonReader.nextString());
	}

	@Override
	public void write(JsonWriter jsonWriter, Fraction fraction) throws IOException {
		jsonWriter.value(fraction.toString(false));
	}
}
