package com.zezadas.flashmod;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;


public class MainActivity extends ActionBarActivity {
    private String TAG = "FlashMod";
    Process process = null;
    private String configFile = "/system/etc/flashled_calc_parameters.cfg";
    private String confArray[];
    private SeekBar seekBar1;
    private SeekBar seekBar2;
    private SeekBar seekBar3;
    private SeekBar seekBar4;
    private TextView textView2;
    private TextView textView4;
    private TextView textView6;
    private TextView textView8;
    private Switch switch1;
    private int max_if_torch_3a = -1;
    private int if_torch_vr = -1;
    private int if_indicator_level = -1;
    private int max_if_flash = -1;
    Camera cam;
    private boolean isFlashOn;
    AlertDialog alertBackup;
    private Menu globalmenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        seekBar3 = (SeekBar) findViewById(R.id.seekBar3);
        seekBar4 = (SeekBar) findViewById(R.id.seekBar4);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView6 = (TextView) findViewById(R.id.textView6);
        textView8 = (TextView) findViewById(R.id.textView8);
        switch1 = (Switch) findViewById(R.id.switch1);
        seekBar1.setMax(30000);
        seekBar2.setMax(30000);
        seekBar3.setMax(30000);
        seekBar4.setMax(30000);


        File backupFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/flashled_calc_parameters.cfg.backup");
        if (!backupFile.exists()) {
            AlertDialog.Builder builderBackup = new AlertDialog.Builder(this);
            builderBackup.setTitle("Seems that you dont have a backup yet?");
            builderBackup.setMessage("Do you want to backup?");
            builderBackup.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    doBackup();
                    dialog.dismiss();
                }
            });
            builderBackup.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getBaseContext(), "Current values have no backup", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            alertBackup = builderBackup.create();
            alertBackup.show();
        }

        File confFileIn = new File(configFile);
        AlertDialog alertConf = null;
        if (!confFileIn.exists()) {
            alertBackup.dismiss();
            AlertDialog.Builder builderConf = new AlertDialog.Builder(this);
            builderConf.setTitle("Seems that there is no config file.");
            builderConf.setMessage("Maybe your device is not supported");
            builderConf.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertConf = builderConf.create();
            alertConf.show();
        }

        AlertDialog alertCheck = null;
        if (!checkRootMethod()) {
            AlertDialog.Builder builderCheck = new AlertDialog.Builder(this);
            builderCheck.setTitle("Seems that Su is not installed");
            builderCheck.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            alertCheck = builderCheck.create();
            alertCheck.show();
        }

        AlertDialog.Builder builderDisclaim = new AlertDialog.Builder(this);
        builderDisclaim.setTitle("I'm not responsible if anything bad happens to your phone...");
        builderDisclaim.setMessage("blah blah blah \nthe usual disclaimer\nPlease visit my XDA Thread :)");
        builderDisclaim.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDisclaim = builderDisclaim.create();
        alertDisclaim.show();

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                               @Override
                                               public void onCheckedChanged(CompoundButton buttonView,
                                                                            boolean isChecked) {

                                                   if (isChecked) {
                                                       cam = Camera.open();
                                                       Camera.Parameters p = cam.getParameters();
                                                       p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                                       cam.setParameters(p);
                                                       cam.startPreview();
                                                       isFlashOn = true;
                                                       switch1.setText("Flash ON");
                                                   } else {
                                                       cam.stopPreview();
                                                       cam.release();
                                                       isFlashOn = false;
                                                       switch1.setText("Flash OFF");
                                                   }
                                               }
                                           }
        );

        if (confFileIn.exists()) {
            LinkedList<String> configList = new LinkedList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(confFileIn));
                String line;
                while ((line = br.readLine()) != null) {
                    configList.add(line);

                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            confArray = configList.toArray(new String[1]);
            for (int i = 0; i < confArray.length; i++) {
                if (confArray[i].contains("max_if_torch_3a")) {
                    max_if_torch_3a = i;
                } else if (confArray[i].contains("if_torch_vr")) {
                    if_torch_vr = i;
                } else if (confArray[i].contains("if_indicator_level")) {
                    if_indicator_level = i;
                } else if (confArray[i].contains("max_if_flash")) {
                    max_if_flash = i;
                }
            }

            populateValues();

            seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

                                                {
                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int progresValue,
                                                                                  boolean fromUser) {
                                                        int amper = (progresValue * 100);
                                                        textView2.setText(String.valueOf(amper) + "/" + "3000000 MicroAmperes");
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                                    }

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                                    }
                                                }

            );

            seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

                                                {
                                                    int progress = 0;

                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int progresValue,
                                                                                  boolean fromUser) {
                                                        int amper = (progresValue * 100);
                                                        textView2.setText(String.valueOf(amper) + "/" + "3000000 MicroAmperes");
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                                    }

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                                    }
                                                }

            );

            seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

                                                {
                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int progresValue,
                                                                                  boolean fromUser) {
                                                        int amper = (progresValue * 100);
                                                        textView4.setText(String.valueOf(amper) + "/" + "3000000 MicroAmperes");
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                                    }

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                                    }
                                                }

            );

            seekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

                                                {
                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int progresValue,
                                                                                  boolean fromUser) {
                                                        int amper = (progresValue * 100);
                                                        textView6.setText(String.valueOf(amper) + "/" + "3000000 MicroAmperes");
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                                    }

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                                    }
                                                }

            );

            seekBar4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()

                                                {
                                                    @Override
                                                    public void onProgressChanged(SeekBar seekBar, int progresValue,
                                                                                  boolean fromUser) {
                                                        int amper = (progresValue * 100);
                                                        textView8.setText(String.valueOf(amper) + "/" + "3000000 MicroAmperes");
                                                    }

                                                    @Override
                                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                                    }

                                                    @Override
                                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                                    }

                                                }
            );

        }

    }

    private static boolean checkPermissions() {
        StringBuffer output;
        try {
// Executes the command.
            Process process = Runtime.getRuntime().exec("su -c \"whoami\"");

// Reads stdout.
// NOTE: You can write to stdin of the command using
//       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

// Waits for the command to finish.
            process.waitFor();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (output.toString().trim().equals("root"))
            return true;

        return false;
    }

    private static boolean checkRootMethod() {
        String[] paths = {"/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su"};
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    private void populateValues() {
        int value = Integer.parseInt(confArray[max_if_torch_3a].split("=")[1].trim());
        int amper = (value) / 100;
        seekBar1.setProgress(amper);
        textView2.setText(String.valueOf(value) + "/" + "3000000 MicroAmperes");

        value = Integer.parseInt(confArray[if_torch_vr].split("=")[1].trim());
        amper = (value) / 100;
        seekBar2.setProgress(amper);
        textView4.setText(String.valueOf(value) + "/" + "3000000 MicroAmperes");

        value = Integer.parseInt(confArray[if_indicator_level].split("=")[1].trim());
        amper = (value) / 100;
        seekBar3.setProgress(amper);
        textView6.setText(String.valueOf(value) + "/" + "3000000 MicroAmperes");

        value = Integer.parseInt(confArray[max_if_flash].split("=")[1].trim());
        amper = (value) / 100;
        seekBar4.setProgress(amper);
        textView8.setText(String.valueOf(value) + "/" + "3000000 MicroAmperes");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        File confFileIn = new File(configFile);
        if (!confFileIn.exists()) {
            menu.findItem(R.id.action_apply).setVisible(false);
            menu.findItem(R.id.action_backup).setVisible(false);
            menu.findItem(R.id.action_restore).setVisible(false);
        }
        return true;
    }

    private void doBackup() {
        DataOutputStream os;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mount -o remount,rw /system\n");
            os.writeBytes("cp -f /system/etc/flashled_calc_parameters.cfg "
                    + Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/flashled_calc_parameters.cfg.backup\n");
            os.writeBytes("chmod 777 " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/flashled_calc_parameters.cfg.backup\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            Toast.makeText(getBaseContext(), "Backup done", Toast.LENGTH_SHORT).show();
        }
    }

    private void backup() {
        File backupFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/flashled_calc_parameters.cfg.backup");
        if (backupFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("You already have a backup.");
            builder.setMessage("Do you want to replace?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    doBackup();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getBaseContext(), "Current values have no backup", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            doBackup();
        }
    }

    private void restore() {
        if (!checkPermissions()) {
            Toast.makeText(getApplicationContext(), "Please confirm root access", Toast.LENGTH_SHORT).show();
            return;
        }
        File backupFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/flashled_calc_parameters.cfg.backup");
        if (!backupFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("There is no Backup File to Restore.");
            builder.setMessage("Do you want to create a new one?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    backup();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getBaseContext(), "There is no file to restore, backup first", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            doRestore();
        }
    }

    private void doRestore() {
        DataOutputStream os;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mount -o remount,rw /system\n");
            os.writeBytes("cp -f "
                    + Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/flashled_calc_parameters.cfg.backup " + "/system/etc/flashled_calc_parameters.cfg" + "\n");
            os.writeBytes("chmod 644 " + "/system/etc/flashled_calc_parameters.cfg\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            File confFileIn = new File(configFile);

                LinkedList<String> configList = new LinkedList<>();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(confFileIn));
                    String line;
                    while ((line = br.readLine()) != null) {
                        configList.add(line);

                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                confArray = configList.toArray(new String[1]);
            for (int i = 0; i < confArray.length; i++) {
                if (confArray[i].contains("max_if_torch_3a")) {
                    max_if_torch_3a = i;
                } else if (confArray[i].contains("if_torch_vr")) {
                    if_torch_vr = i;
                } else if (confArray[i].contains("if_indicator_level")) {
                    if_indicator_level = i;
                } else if (confArray[i].contains("max_if_flash")) {
                    max_if_flash = i;
                }
            }

            populateValues();
            Toast.makeText(getBaseContext(), "Values have been restored", Toast.LENGTH_SHORT).show();
        }
    }


    private void apply() {
        if (!checkPermissions()) {
            Toast.makeText(getApplicationContext(), "Please confirm root access", Toast.LENGTH_SHORT).show();
            return;
        }
        File backupFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/flashled_calc_parameters.cfg.backup");
        if (!backupFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("There is no Backup File.");
            builder.setMessage("Do you want to create one?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    backup();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getBaseContext(), "Original values will lost", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        if (max_if_torch_3a != -1) {
            confArray[max_if_torch_3a] = "max_if_torch_3a = " + String.valueOf(seekBar1.getProgress() * 100);
        }
        if (if_torch_vr != -1) {
            confArray[if_torch_vr] = "if_torch_vr = " + String.valueOf(seekBar2.getProgress() * 100);
        }
        if (if_indicator_level != -1) {
            confArray[if_indicator_level] = "if_indicator_level = " + String.valueOf(seekBar3.getProgress() * 100);
        }
        if (max_if_flash != -1) {
            confArray[max_if_flash] = "max_if_flash = " + String.valueOf(seekBar4.getProgress() * 100);
        }
        BufferedWriter bw;
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/flashled_calc_parameters.cfg.tmp");
            file.createNewFile();
            bw = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < confArray.length; i++) {
                bw.write(confArray[i]);
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataOutputStream os;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mount -o remount,rw /system\n");
            os.writeBytes("cp -f "
                    + Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/flashled_calc_parameters.cfg.tmp " + "/system/etc/flashled_calc_parameters.cfg" + "\n");
            os.writeBytes("chmod 644 " + "/system/etc/flashled_calc_parameters.cfg.tmp\n");
            os.writeBytes("rm" + Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/flashled_calc_parameters.cfg.tmp" + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            reboot();
            Toast.makeText(getBaseContext(), "Values have been applied\n Please Reboot", Toast.LENGTH_SHORT).show();
        }
    }

    private void reboot() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reboot to apply new Values");
        builder.setMessage("Do you want to continue?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DataOutputStream os;
                try {
                    process = Runtime.getRuntime().exec("su");
                    os = new DataOutputStream(process.getOutputStream());
                    os.writeBytes("busybox killall zygote\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    process.waitFor();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_backup) {
            backup();
        } else if (id == R.id.action_apply) {
            apply();
        } else if (id == R.id.action_restore) {
            restore();
        } else if (id == R.id.action_reboot) {
            reboot();
        } else if (id == R.id.action_about) {
            Toast.makeText(getApplicationContext(), "Visit my XDA thread and press thanks ;)", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
      /*
       //DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mount -o remount,rw /system\n");


            File flashConfigFile = new File("/system/etc/flashled_calc_parameters.cfg");

            if(!flashConfigFile.exists()){

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Seems that there is no config file.");
                builder.setMessage("Maybe your device is not supported");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return ;
                        //dialog.dismiss();
                    }
                });

            }

            os.writeBytes("cp -f /system/etc/flashled_calc_parameters.cfg "
                        + Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/flashled_calc_parameters.cfg.tmp\n");
            os.writeBytes("chmod 777 " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/flashled_calc_parameters.cfg.tmp\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
*/