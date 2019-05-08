package io.intelehealth.client.views.activites;

import android.app.DatePickerDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.intelehealth.client.R;
import io.intelehealth.client.dao.PatientsDAO;
import io.intelehealth.client.database.InteleHealthDatabaseHelper;
import io.intelehealth.client.databinding.ActivityIdentificationBinding;
import io.intelehealth.client.exception.DAOException;
import io.intelehealth.client.utilities.DateAndTimeUtils;
import io.intelehealth.client.utilities.EditTextUtils;
import io.intelehealth.client.utilities.FileUtils;
import io.intelehealth.client.utilities.Logger;
import io.intelehealth.client.utilities.SessionManager;
import io.intelehealth.client.utilities.UuidGenerator;
import io.intelehealth.client.viewModels.IdentificationViewModel;
import io.intelehealth.client.objects.Patient;

public class IdentificationActivity extends AppCompatActivity {
    private static final String TAG = IdentificationActivity.class.getSimpleName();
    ActivityIdentificationBinding binding;
    IdentificationViewModel identificationViewModel;
    SessionManager sessionManager = null;
    InteleHealthDatabaseHelper mDbHelper = null;
    private boolean hasLicense = false;
    private ArrayAdapter<CharSequence> educationAdapter;
    private ArrayAdapter<CharSequence> economicStatusAdapter;
    UuidGenerator uuidGenerator = new UuidGenerator();
    Calendar today = Calendar.getInstance();
    Calendar dob = Calendar.getInstance();
    Patient patient1 = new Patient();
    private String patientUuid = "";
    private String mGender;
    String patientID_edit;
    private int mDOBYear;
    private int mDOBMonth;
    private int mDOBDay;
    private DatePickerDialog mDOBPicker;
    private int mAgeYears = 0;
    private int mAgeMonths = 0;
    private AlertDialog.Builder mAgePicker;
    private String country1;
    Spinner mCountry;
    Spinner mState;
    AutoCompleteTextView mCity;
    PatientsDAO patientsDAO = new PatientsDAO();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_identification);
        identificationViewModel = ViewModelProviders.of(this).get(IdentificationViewModel.class);
        binding.setIdentificationViewModel(identificationViewModel);
        binding.setLifecycleOwner(this);

        setTitle(R.string.title_activity_identification);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        sessionManager = new SessionManager(this);
        mState = findViewById(R.id.spinner_state);
        mCountry = findViewById(R.id.spinner_country);
        mCity = findViewById(R.id.identification_city);


//Initialize the local database to store patient information
        mDbHelper = new InteleHealthDatabaseHelper(this);

        Intent intent = this.getIntent(); // The intent was passed to the activity
        if (intent != null) {
            if (intent.hasExtra("patientUuid")) {
                this.setTitle("Update Patient");
                patientID_edit = intent.getStringExtra("patientUuid");
                patient1.setUuid(patientID_edit);
                setscreen(patientID_edit);
            }
        }
        if (sessionManager.valueContains("licensekey"))
            hasLicense = true;
        try {
            JSONObject obj = null;
            String mFileName = "config.json";
            if (hasLicense) {
                obj = new JSONObject(FileUtils.readFileRoot(mFileName, this)); //Load the config file

            } else {
                obj = new JSONObject(String.valueOf(FileUtils.encodeJSON(this, mFileName)));

            }
            country1 = obj.getString("mCountry");

            if (country1.equalsIgnoreCase("India")) {
                EditTextUtils.setEditTextMaxLength(10, binding.identificationPhoneNumber);
            } else if (country1.equalsIgnoreCase("Philippines")) {
                EditTextUtils.setEditTextMaxLength(11, binding.identificationPhoneNumber);
            }

        } catch (JSONException e) {
            e.printStackTrace();
//            Issue #627
//            added the catch exception to check the config and throwing back to setup activity
            Toast.makeText(getApplicationContext(), "JsonException" + e, Toast.LENGTH_LONG).show();
            showAlertDialogButtonClicked(e.toString());
        }
        Resources res = getResources();
        ArrayAdapter<CharSequence> countryAdapter = ArrayAdapter.createFromResource(this,
                R.array.countries, android.R.layout.simple_spinner_item);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCountry.setAdapter(countryAdapter);

        ArrayAdapter<CharSequence> casteAdapter = ArrayAdapter.createFromResource(this,
                R.array.caste, android.R.layout.simple_spinner_item);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCaste.setAdapter(casteAdapter);
        try {
            String economicLanguage = "economic_" + Locale.getDefault().getLanguage();
            int economics = res.getIdentifier(economicLanguage, "array", getApplicationContext().getPackageName());
            if (economics != 0) {
                economicStatusAdapter = ArrayAdapter.createFromResource(this,
                        economics, android.R.layout.simple_spinner_item);
            }
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerEconomicStatus.setAdapter(economicStatusAdapter);
        } catch (Exception e) {
            Toast.makeText(this, "Economic values are missing", Toast.LENGTH_SHORT).show();
            Logger.logE("Identification", "#648", e);
        }
        try {
            String educationLanguage = "education_" + Locale.getDefault().getLanguage();
            int educations = res.getIdentifier(educationLanguage, "array", getApplicationContext().getPackageName());
            if (educations != 0) {
                educationAdapter = ArrayAdapter.createFromResource(this,
                        educations, android.R.layout.simple_spinner_item);

            }
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerEducation.setAdapter(educationAdapter);
        } catch (Exception e) {
            Toast.makeText(this, "Education values are missing", Toast.LENGTH_SHORT).show();
            Logger.logE("Identification", "#648", e);
        }


        if (null == patientID_edit || patientID_edit.isEmpty()) {
            generateUuid();

        }

        // setting radio button automatically according to the databse when user clicks edit details
        if (patientID_edit != null) {

            if (patient1.getGender().equals("M")) {
                binding.identificationGenderMale.setChecked(true);
                if (binding.identificationGenderFemale.isChecked())
                    binding.identificationGenderFemale.setChecked(false);
                Log.v(TAG, "yes");
            } else {
                binding.identificationGenderFemale.setChecked(true);
                if (binding.identificationGenderMale.isChecked())
                    binding.identificationGenderMale.setChecked(false);
                Log.v(TAG, "yes");
            }

        }
        if (binding.identificationGenderMale.isChecked()) {
            mGender = "M";
        } else {
            mGender = "F";
        }
        if (patientID_edit != null) {
            // setting country according database
            binding.spinnerCountry.setSelection(countryAdapter.getPosition(String.valueOf(patient1.getCountry())));
            if (patient1.getEducation_level().equals(getString(R.string.not_provided)))
                binding.spinnerEducation.setSelection(0);
            else
                binding.spinnerEducation.setSelection(educationAdapter.getPosition(patient1.getEducation_level()));
            if (patient1.getEconomic_status().equals(getString(R.string.not_provided)))
                binding.spinnerEconomicStatus.setSelection(0);
            else
                binding.spinnerEconomicStatus.setSelection(economicStatusAdapter.getPosition(patient1.getEconomic_status()));
            if (patient1.getCaste().equals(getString(R.string.not_provided)))
                binding.spinnerCaste.setSelection(0);
            else
                binding.spinnerCaste.setSelection(casteAdapter.getPosition(patient1.getCaste()));
        } else {
            binding.spinnerCountry.setSelection(countryAdapter.getPosition(country1));
        }

        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(this, R.array.state_error, android.R.layout.simple_spinner_item);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mState.setAdapter(stateAdapter);

        mState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String state = parent.getItemAtPosition(position).toString();
                if (state.matches("Odisha")) {
                    //Creating the instance of ArrayAdapter containing list of fruit names
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(IdentificationActivity.this,
                            R.array.odisha_villages, android.R.layout.simple_spinner_item);
                    mCity.setThreshold(1);//will start working from first character
                    mCity.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
                } else if (state.matches("Bukidnon")) {
                    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(IdentificationActivity.this,
                            R.array.bukidnon_villages, android.R.layout.simple_spinner_item);
                    mCity.setThreshold(1);//will start working from first character
                    mCity.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView
                } else {
                    mCity.setAdapter(null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != 0) {
                    String country = adapterView.getItemAtPosition(i).toString();

                    if (country.matches("India")) {
                        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(IdentificationActivity.this,
                                R.array.states_india, android.R.layout.simple_spinner_item);
                        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mState.setAdapter(stateAdapter);
                        // setting state according database when user clicks edit details

                        if (patientID_edit != null) {
                            mState.setSelection(stateAdapter.getPosition(String.valueOf(patient1.getState_province())));
                        } else {
                            mState.setSelection(stateAdapter.getPosition("Odisha"));
                        }

                    } else if (country.matches("United States")) {
                        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(IdentificationActivity.this,
                                R.array.states_us, android.R.layout.simple_spinner_item);
                        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mState.setAdapter(stateAdapter);

                        if (patientID_edit != null) {

                            mState.setSelection(stateAdapter.getPosition(String.valueOf(patient1.getState_province())));
                        }
                    } else if (country.matches("Philippines")) {
                        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(IdentificationActivity.this,
                                R.array.states_philippines, android.R.layout.simple_spinner_item);
                        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        mState.setAdapter(stateAdapter);

                        if (patientID_edit != null) {
                            mState.setSelection(stateAdapter.getPosition(String.valueOf(patient1.getState_province())));
                        } else {
                            mState.setSelection(stateAdapter.getPosition("Bukidnon"));
                        }
                    }
                } else {
                    ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(IdentificationActivity.this,
                            R.array.state_error, android.R.layout.simple_spinner_item);
                    stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mState.setAdapter(stateAdapter);
                }
                identificationViewModel.state.setValue(mState.getSelectedItem().toString());
                identificationViewModel.country.setValue(mCountry.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        binding.identificationGenderFemale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRadioButtonClicked(v);
            }
        });

        binding.identificationGenderMale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRadioButtonClicked(v);
            }
        });
        binding.imageviewIdPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String[] results = HelperMethods.startImageCapture(IdentificationActivity.this,
                //        IdentificationActivity.this);
                //if (results != null) {
                //    mPhoto = results[0];
                //    mCurrentPhotoPath = results[1];
                //}
                File filePath = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator +
                        "Patient_Images" + File.separator + patientUuid);
                if (!filePath.exists()) {
                    filePath.mkdir();
                }
                Intent cameraIntent = new Intent(IdentificationActivity.this, CameraActivity.class);

                // cameraIntent.putExtra(CameraActivity.SHOW_DIALOG_MESSAGE, getString(R.string.camera_dialog_default));
                cameraIntent.putExtra(CameraActivity.SET_IMAGE_NAME, patientUuid);
                cameraIntent.putExtra(CameraActivity.SET_IMAGE_PATH, filePath);
                startActivityForResult(cameraIntent, CameraActivity.TAKE_IMAGE);
            }
        });
        mDOBYear = today.get(Calendar.YEAR);
        mDOBMonth = today.get(Calendar.MONTH);
        mDOBDay = today.get(Calendar.DAY_OF_MONTH);
        //DOB is set using an AlertDialog
        mDOBPicker = new DatePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                //Set the DOB calendar to the date selected by the user
                dob.set(year, monthOfYear, dayOfMonth);
                binding.identificationBirthDateTextView.setError(null);
                binding.identificationAge.setError(null);
                //Set Maximum date to current date because even after bday is less than current date it goes to check date is set after today
                mDOBPicker.getDatePicker().setMaxDate(System.currentTimeMillis() - 1000);

                //Formatted so that it can be read the way the user sets
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-mm-yyyy", Locale.getDefault());
                dob.set(year, monthOfYear, dayOfMonth);
                String dobString = simpleDateFormat.format(dob.getTime());
                binding.identificationBirthDateTextView.setText(dobString);

                mAgeYears = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
                mAgeMonths = today.get(Calendar.MONTH) - dob.get(Calendar.MONTH);


                if (mAgeMonths < 0) {
                    mAgeMonths = mAgeMonths + 12;
                    mAgeYears = mAgeYears - 1;
                }

                if (mAgeMonths < 0 || mAgeYears < 0 || dob.after(today)) {
                    binding.identificationBirthDateTextView.setError(getString(R.string.identification_screen_error_dob));
                    binding.identificationAge.setError(getString(R.string.identification_screen_error_age));
                    return;
                }

                mDOBYear = year;
                mDOBMonth = monthOfYear;
                mDOBDay = dayOfMonth;

                String ageString = mAgeYears + getString(R.string.identification_screen_text_years) + mAgeMonths + getString(R.string.identification_screen_text_months);
                binding.identificationAge.setText(ageString);
            }
        }, mDOBYear, mDOBMonth, mDOBDay);

        //DOB Picker is shown when clicked
        binding.identificationBirthDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDOBPicker.show();
            }
        });

        //if patient update then age will be set
        if (patientID_edit != null) {
            int age = DateAndTimeUtils.getAge(patient1.getDate_of_birth());
            binding.identificationBirthDateTextView.setText(patient1.getDate_of_birth());
            int month = DateAndTimeUtils.getMonth(patient1.getDate_of_birth());
            binding.identificationAge.setText(age + getString(R.string.identification_screen_text_years) + month + getString(R.string.identification_screen_text_months));
        }
        binding.identificationAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAgePicker = new AlertDialog.Builder(IdentificationActivity.this, R.style.AlertDialogStyle);
                mAgePicker.setTitle(R.string.identification_screen_prompt_age);
                final LayoutInflater inflater = getLayoutInflater();
                View convertView = inflater.inflate(R.layout.dialog_2_numbers_picker, null);
                mAgePicker.setView(convertView);
                final NumberPicker yearPicker = convertView.findViewById(R.id.dialog_2_numbers_quantity);
                final NumberPicker monthPicker = convertView.findViewById(R.id.dialog_2_numbers_unit);
                final TextView middleText = convertView.findViewById(R.id.dialog_2_numbers_text);
                final TextView endText = convertView.findViewById(R.id.dialog_2_numbers_text_2);
                middleText.setText(getString(R.string.identification_screen_picker_years));
                endText.setText(getString(R.string.identification_screen_picker_months));
                yearPicker.setMinValue(0);
                yearPicker.setMaxValue(100);
                monthPicker.setMinValue(0);
                monthPicker.setMaxValue(12);
                if (mAgeYears > 0) {
                    yearPicker.setValue(mAgeYears);
                }
                if (mAgeMonths > 0) {
                    monthPicker.setValue(mAgeMonths);
                }

                mAgePicker.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        yearPicker.setValue(yearPicker.getValue());
                        monthPicker.setValue(monthPicker.getValue());
                        String ageString = yearPicker.getValue() + getString(R.string.identification_screen_text_years) + monthPicker.getValue() + getString(R.string.identification_screen_text_months);
                        binding.identificationAge.setText(ageString);


                        Calendar calendar = Calendar.getInstance();
                        int curYear = calendar.get(Calendar.YEAR);
                        int birthYear = curYear - yearPicker.getValue();
                        int curMonth = calendar.get(Calendar.MONTH);
                        int birthMonth = curMonth - monthPicker.getValue();
                        mDOBYear = birthYear;
                        mDOBMonth = birthMonth;
                        mDOBDay = 1;

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
                        dob.set(mDOBYear, mDOBMonth, mDOBDay);
                        String dobString = simpleDateFormat.format(dob.getTime());
                        binding.identificationBirthDateTextView.setText(dobString);
                        mDOBPicker.updateDate(mDOBYear, mDOBMonth, mDOBDay);
                        dialog.dismiss();
                    }
                });
                mAgePicker.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                mAgePicker.show();
            }
        });
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (patientID_edit != null) {
                    identificationViewModel.onPatientUpdateClicked(patient1);
                } else {
                    identificationViewModel.onPatientCreateClicked();
                }
            }
        });
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.identification_gender_male:
                if (checked)
                    mGender = "M";
                Log.v(TAG, "gender:" + mGender);
                identificationViewModel.gender.setValue(mGender);
                break;
            case R.id.identification_gender_female:
                if (checked)
                    mGender = "F";
                Log.v(TAG, "gender:" + mGender);
                identificationViewModel.gender.setValue(mGender);
                break;
        }
    }

    public void generateUuid() {

        patientUuid = uuidGenerator.UuidGenerator();

    }

    // This method is for setting the screen with existing values in database whenn user clicks edit details
    private void setscreen(String str) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String patientSelection = "uuid=?";
        String[] patientArgs = {str};
        String[] patientColumns = {"uuid","first_name", "middle_name", "last_name",
                "date_of_birth", "address1", "address2", "city_village", "state_province",
                "postal_code", "country", "phone_number", "gender", "sdw", "occupation", "patient_photo",
                "economic_status", "education_status", "caste"};
        Cursor idCursor = db.query("tbl_patient", patientColumns, patientSelection, patientArgs, null, null, null);
        if (idCursor.moveToFirst()) {
            do {
                patient1.setUuid(idCursor.getString(idCursor.getColumnIndexOrThrow("uuid")));
                patient1.setFirst_name(idCursor.getString(idCursor.getColumnIndexOrThrow("first_name")));
                patient1.setMiddle_name(idCursor.getString(idCursor.getColumnIndexOrThrow("middle_name")));
                patient1.setLast_name(idCursor.getString(idCursor.getColumnIndexOrThrow("last_name")));
                patient1.setDate_of_birth(idCursor.getString(idCursor.getColumnIndexOrThrow("date_of_birth")));
                patient1.setAddress1(idCursor.getString(idCursor.getColumnIndexOrThrow("address1")));
                patient1.setAddress2(idCursor.getString(idCursor.getColumnIndexOrThrow("address2")));
                patient1.setCity_village(idCursor.getString(idCursor.getColumnIndexOrThrow("city_village")));
                patient1.setState_province(idCursor.getString(idCursor.getColumnIndexOrThrow("state_province")));
                patient1.setPostal_code(idCursor.getString(idCursor.getColumnIndexOrThrow("postal_code")));
                patient1.setCountry(idCursor.getString(idCursor.getColumnIndexOrThrow("country")));
                patient1.setPhone_number(idCursor.getString(idCursor.getColumnIndexOrThrow("phone_number")));
                patient1.setGender(idCursor.getString(idCursor.getColumnIndexOrThrow("gender")));
                patient1.setSdw(idCursor.getString(idCursor.getColumnIndexOrThrow("sdw")));
                patient1.setOccupation(idCursor.getString(idCursor.getColumnIndexOrThrow("occupation")));
                patient1.setPatient_photo(idCursor.getString(idCursor.getColumnIndexOrThrow("patient_photo")));
                patient1.setEconomic_status(idCursor.getString(idCursor.getColumnIndexOrThrow("economic_status")));
                patient1.setEducation_level(idCursor.getString(idCursor.getColumnIndexOrThrow("education_status")));
                patient1.setCaste(idCursor.getString(idCursor.getColumnIndexOrThrow("caste")));

            } while (idCursor.moveToNext());
            idCursor.close();
        }
        String patientSelection1 = "patientuuid = ?";
        String[] patientArgs1 = {str};
        String[] patientColumns1 = {"value", "person_attribute_type_uuid"};
        final Cursor idCursor1 = db.query("tbl_patient_attribute", patientColumns1, patientSelection1, patientArgs1, null, null, null);
        String name = "";
        if (idCursor1.moveToFirst()) {
            do {
                try {
                    name = patientsDAO.getAttributesName(idCursor1.getString(idCursor1.getColumnIndexOrThrow("person_attribute_type_uuid")));
                } catch (DAOException e) {
                    e.printStackTrace();
                }

                if (name.equalsIgnoreCase("caste")) {
                    patient1.setCaste(idCursor1.getString(idCursor1.getColumnIndexOrThrow("value")));
                }
                if (name.equalsIgnoreCase("Telephone Number")) {
                    patient1.setPhone_number(idCursor1.getString(idCursor1.getColumnIndexOrThrow("value")));
                }
                if (name.equalsIgnoreCase("Education Level")) {
                    patient1.setEducation_level(idCursor1.getString(idCursor1.getColumnIndexOrThrow("value")));
                }
                if (name.equalsIgnoreCase("Economic Status")) {
                    patient1.setEconomic_status(idCursor1.getString(idCursor1.getColumnIndexOrThrow("value")));
                }
                if (name.equalsIgnoreCase("occupation")) {
                    patient1.setOccupation(idCursor1.getString(idCursor1.getColumnIndexOrThrow("value")));
                }
                if (name.equalsIgnoreCase("Son/wife/daughter")) {
                    patient1.setSdw(idCursor1.getString(idCursor1.getColumnIndexOrThrow("value")));
                }

            } while (idCursor1.moveToNext());
        }
        idCursor1.close();
        identificationViewModel.firstname.setValue(patient1.getFirst_name());
        identificationViewModel.middlename.setValue(patient1.getMiddle_name());
        identificationViewModel.lastname.setValue(patient1.getLast_name());
        identificationViewModel.dateofbirth.setValue(patient1.getDate_of_birth());
        identificationViewModel.phonenumber.setValue(patient1.getPhone_number());
        identificationViewModel.village.setValue(patient1.getCity_village());
        identificationViewModel.address.setValue(patient1.getAddress1());
        identificationViewModel.address2.setValue(patient1.getAddress2());
        identificationViewModel.postalcode.setValue(patient1.getPostal_code());
        identificationViewModel.sondaughter.setValue(patient1.getSdw());
        identificationViewModel.occupation.setValue(patient1.getOccupation());

    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to go back ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).setNegativeButton("No", null).show();

    }

    public void showAlertDialogButtonClicked(String errorMessage) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Config Error");
        alertDialogBuilder.setMessage(errorMessage);
        alertDialogBuilder.setNeutralButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                Intent i = new Intent(IdentificationActivity.this, SetupActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);// This flag ensures all activities on top of the CloseAllViewsDemo are cleared.
                startActivity(i);
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
