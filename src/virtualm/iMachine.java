/* $Id: TestVBox.java 135976 2020-02-04 10:35:17Z bird $ */
 /*! file
 * Small sample/testcase which demonstrates that the same source code can
 * be used to connect to the webservice and (XP)COM APIs.
 */

 /*
 * Copyright (C) 2010-2020 Oracle Corporation
 *
 * This file is part of VirtualBox Open Source Edition (OSE), as
 * available from http://www.virtualbox.org. This file is free software;
 * you can redistribute it and/or modify it under the terms of the GNU
 * General Public License (GPL) as published by the Free Software
 * Foundation, in version 2 as it comes in the "COPYING" file of the
 * VirtualBox OSE distribution. VirtualBox OSE is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY of any kind.
 */
import org.virtualbox_6_1.*;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.math.BigInteger;
import sun.awt.OSInfo;

public class iMachine {

    static boolean progressStatus(VirtualBoxManager mgr, IProgress p, long waitMillis) {
        long end = System.currentTimeMillis() + waitMillis;
        while (!p.getCompleted()) {
            // process system event queue
            /*
            doc says:
            It is good practice in long-running API clients to process the system events every now and then
            in the main thread (does not work in other threads). As a rule of thumb it makes sense to process
            them every few 100msec to every few seconds). This is done by calling
             */
            mgr.waitForEvents(0);

            /*
            doc:
            Waits until the task is done (including all sub-operations) with a given timeout in milliseconds;
            specify -1 for an indefinite wait.
             */
            p.waitForCompletion(-1);
            if (System.currentTimeMillis() >= end) {
                return false;
            }
        }
        //to check progress status
        System.out.println(p.getDescription());
        if (p.getErrorInfo() != null) {
            System.out.println(p.getErrorInfo().getText());
        }
        return true;
    }

    static void turnOn_VM(VirtualBoxManager virtualBoxManager, IVirtualBox vbox) {
        //from the list returned by getMachines we get the third one
        IMachine iMachine = vbox.getMachines().get(2);
        IMachine cobe = vbox.findMachine("betaMachine");
        String name = iMachine.getName();
        System.out.println("\nAttempting to start VM '" + name + "'");

        /*
        doc:The ISession interface represents a client process and allows for locking virtual machines 
        (represented by IMachine objects) to prevent conflicting changes to the machine.
        
        Any caller wishing to manipulate a virtual machine needs to create a session object first, which
        lives in its own process space. Such session objects are then associated with IMachine objects
        living in the VirtualBox server process to coordinate such changes
         */
        ISession session = virtualBoxManager.getSessionObject();
        ArrayList<String> env = new ArrayList<String>();
        /*
        To launch a virtual machine, you call IMachine::launchVMProcess(). In doing so, the caller
        instructs the VirtualBox engine to start a new process with the virtual machine in it, since to the
        host, each virtual machine looks like single process, even if it has hundreds of its own processes
        inside. (This new VM process in turn obtains a write lock on the machine, as described above,
        to prevent conflicting changes from other processes; this is why opening another session will fail
        while the VM is running.)
         */
        //Param 1 session object
        //param 2 session type
        //Param 3 possibly environment setting
        IProgress launchVMProcess = cobe.launchVMProcess(session, "gui", env);
//        IProgress p = iMachine.launchVMProcess(session, "gui", env);
        //give the proces 10 secs
        System.out.println("launching Vm!");
        launchVMProcess.waitForCompletion(-1);
//        progressStatus(virtualBoxManager, p, 10000);

//        session.unlockMachine();
        // process system event queue
//        virtualBoxManager.waitForEvents(0);
    }

    static void turnOff_VM(VirtualBoxManager virtualBoxManager, IVirtualBox vbox) {
        /*
        doc:Attempts to find a virtual machine given its name or UUID.
         */
        IMachine machine = vbox.findMachine("fedora33");

        ISession session = virtualBoxManager.getSessionObject();
        /*
         If set to Write, then attempt to acquire an exclusive write lock or fail. If set to Shared,
            then either acquire an exclusive write lock or establish a link to an existing session.
         */
        machine.lockMachine(session, LockType.Shared);

        IProgress powerDown = session.getConsole().powerDown();
        progressStatus(virtualBoxManager, powerDown, 10000);

    }

    static void createMachine(VirtualBoxManager boxManager, IVirtualBox iVirtualBox) {
        /*
        doc says:
        IMachine IVirtualBox::createMachine(
        [in] wstring settingsFile,
        [in] wstring name,
        [in] wstring groups[],
        [in] wstring osTypeId,
        [in] wstring flags)

         */
        ISession session = boxManager.getSessionObject();
        try {
            IMachine AwesomeMachine = iVirtualBox.findMachine("TestMachine");
            AwesomeMachine.lockMachine(session, LockType.Write);
            session.unlockMachine();
            List<IMedium> media = AwesomeMachine.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
            AwesomeMachine.deleteConfig(media);
            System.out.println("The previous machine 'TestMachine' has been deleted successfully ");
        } catch (Exception e) {
            System.out.println("couldn't find machine " + e.getMessage());
        }

//        AwesomeMachine.lockMachine(session, LockType.VM);
        IMachine newMachine = iVirtualBox.createMachine(
                null/*settingsFile*/,
                "TestMachine",
                null/*groups[]*/,
                "Fedora_64",
                "forceOverwrite=1"/*flags*/
        );
        newMachine.saveSettings();
        boxManager.getVBox().registerMachine(newMachine);

//        ISession session = boxManager.getSessionObject();
        newMachine.lockMachine(session, LockType.Write); // machine is now locked for writing
        IMachine mutable = session.getMachine(); // obtain the mutable machine copy
        IGraphicsAdapter iGraphicsAdapter = session.getMachine().getGraphicsAdapter();
        mutable.setMemorySize(4096L);
        iGraphicsAdapter.setGraphicsControllerType(GraphicsControllerType.VMSVGA);
        iGraphicsAdapter.setVRAMSize(128L);
//        mutable.set
//    hdd = ctx[’vb’].createMedium(format, loc, ctx[’global’].constants.AccessMode_ReadWrite, \
//ctx[’global’].constants.DeviceType_HardDisk)

        session.unlockMachine();
        IMedium iMedium = iVirtualBox.createMedium("vdi", "C:\\Users\\Desktop\\VirtualBox VMs\\TestMachine\\TestMachine3.vdi", AccessMode.ReadWrite, DeviceType.HardDisk);
        IMedium iMediumOpened = iVirtualBox.openMedium("C:\\Users\\Desktop\\VirtualBox VMs\\TestMachine\\TestMachine3.vdi", DeviceType.HardDisk, AccessMode.ReadWrite, false);

//        iMedium.deleteStorage();
//        progress = hdd.createBaseStorage(size, (ctx[’global’].constants.MediumVariant_Standard, ))
        List<MediumVariant> variant = new ArrayList<MediumVariant>();
        variant.add(MediumVariant.Standard);
        IProgress createBaseStorage = iMedium.createBaseStorage(15L * 1024L * 1024L * 1024L, variant);
        progressStatus(boxManager, createBaseStorage, 100000);
//        newMachine.attachDevice("SATA Controller", 0, 0, DeviceType.HardDisk, iMediumOpened);
        mutable.attachDevice("SAta Controller", 0, 0, DeviceType.HardDisk, iMediumOpened);
        mutable.saveSettings(); // write settings to XML
    }

    static void printErrorInfo(VBoxException e) {
        System.out.println("VBox error: " + e.getMessage());
        System.out.println("Error cause message: " + e.getCause());
        System.out.println("Overall result code: " + Integer.toHexString(e.getResultCode()));
        int i = 1;
        for (IVirtualBoxErrorInfo ei = e.getVirtualBoxErrorInfo(); ei != null; ei = ei.getNext(), i++) {
            System.out.println("Detail information #" + i);
            System.out.println("Error mesage: " + ei.getText());
            System.out.println("Result code:  " + Integer.toHexString(ei.getResultCode()));
            // optional, usually provides little additional information:
            System.out.println("Component:    " + ei.getComponent());
            System.out.println("Interface ID: " + ei.getInterfaceID());
        }
    }

    /*
    To unregister and delete config
     */
    public static void deleteMachine(VirtualBoxManager virtualBoxManager, IVirtualBox iVirtualBox, ISession iSession) {
        try {
            IMachine machineToDelete = iVirtualBox.findMachine("betaMachine");
            machineToDelete.lockMachine(iSession, LockType.Write);
            iSession.unlockMachine();
            List<IMedium> media = machineToDelete.unregister(CleanupMode.DetachAllReturnHardDisksOnly);
            for (IMedium iMedium : media) {
                System.out.println(iMedium.getName() + " - " + iMedium.getDescription() + " - " + iMedium.getFormat());
            }
            machineToDelete.deleteConfig(media);
            System.out.println("COBE MESSAGE: The previous machine 'betaMachine' has been deleted successfully ");
        } catch (Exception e) {
            System.out.println("COBE MESSAGE: Couldn't find machine " + e.getMessage());
        }
    }

    public static void createVMbeta(VirtualBoxManager virtualBoxManager, IVirtualBox iVirtualBox) {

        ISession session = virtualBoxManager.getSessionObject();
        //Before creating, first delete if exist another machine already created with this procedure.
        deleteMachine(virtualBoxManager, iVirtualBox, session);

        IMachine betaMachine = iVirtualBox.createMachine(null, "betaMachine", null, null, null);

        betaMachine.setMemorySize(1024L);
        betaMachine.setOSTypeId("Fedora_64");

        iVirtualBox.registerMachine(betaMachine);

        betaMachine.lockMachine(session, LockType.Write);

        IMachine sessionMachine = session.getMachine();

        try {
//        IMedium hardDisk = iVirtualBox.createMedium("vdi", "C:\\Users\\Desktop\\VirtualBox VMs\\TestMachine\\TestMachineBetaCobe2.vdi", AccessMode.ReadWrite, DeviceType.HardDisk);
            IMedium hd = iVirtualBox.openMedium("C:\\Users\\Desktop\\VirtualBox VMs\\TestMachine\\TestMachineBetaCobe2.vdi", DeviceType.HardDisk, AccessMode.ReadWrite, Boolean.FALSE);
            List<MediumVariant> mediumVariants = new ArrayList<>();

            mediumVariants.add(MediumVariant.Standard);
            //15Gb

//            IProgress iProgress = hardDisk.createBaseStorage(15L * 1024L * 1024L * 1024L, mediumVariants);
//        iProgress.waitForCompletion(-1);
//        Integer resultCode = iProgress.getResultCode();
//        System.out.println("COBE MESSAGE: ResultCode: " + resultCode);
            sessionMachine.addStorageController("COBE Controller", StorageBus.SATA);

            sessionMachine.attachDevice("COBE Controller", 0, 0, DeviceType.HardDisk, hd);

            // For dvd Image
            sessionMachine.addStorageController("COBE DVD Controller", StorageBus.IDE);

            IMedium dvdImage = iVirtualBox.openMedium("C:\\Users\\Desktop\\Downloads\\Fedora-Server-dvd-x86_64-33-1.2.iso", DeviceType.DVD, AccessMode.ReadOnly, Boolean.FALSE);
            
            sessionMachine.attachDevice("COBE DVD Controller", 1, 0, DeviceType.DVD, dvdImage);
//            sessionMachine.mountMedium("COBE DVD Controller", 1, 0, dvdImage, Boolean.FALSE);
            sessionMachine.setBootOrder(1L, DeviceType.DVD);
            sessionMachine.saveSettings();
            
//            deleteMachine(virtualBoxManager, iVirtualBox, session);

        } catch (Exception e) {
            e.printStackTrace();
        }
        session.unlockMachine();

    }

    public static void main(String[] args) {
        //Create a new instance of VirtualBoxManager
        //Param 1 = homeName
        VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
        //Setting some variables

        boolean ws = false;
        String url = "http://localhost:18083";
        String user = null;
        String passwd = null;

        try {
            virtualBoxManager.connect(url, user, passwd);
        } catch (VBoxException e) {
            e.printStackTrace();
            System.out.println("Cannot connect, start webserver first!");
        }

        try {
            IVirtualBox iVirtualBox = virtualBoxManager.getVBox();

            /*
            to check list of OS's
            List<IGuestOSType> guestOSTypes = iVirtualBox.getGuestOSTypes();
            for (IGuestOSType guestOSType : guestOSTypes) {
                System.out.println("os_type "+guestOSType.getDescription() + " id: "+guestOSType.getId());
            }
            if(true)
                return;
             */
            if (iVirtualBox != null) {
                System.out.println("VirtualBox version: " + iVirtualBox.getVersion() + "\n");
//                turnOn_VM(virtualBoxManager, iVirtualBox);
                IMachine myFedora33 = iVirtualBox.findMachine("fedora33");
                System.out.println("to turn off the machine, press Enter...");

                int ch = System.in.read();
//                turnOff_VM(virtualBoxManager, iVirtualBox);
                System.out.println("to create a new machine, press Enter...");
                ch = System.in.read();
//                createMachine(virtualBoxManager, iVirtualBox);
                createVMbeta(virtualBoxManager, iVirtualBox);
                turnOn_VM(virtualBoxManager, iVirtualBox);
                turnOff_VM(virtualBoxManager, iVirtualBox);
                

            }
        } catch (VBoxException e) {
            printErrorInfo(e);
            System.out.println("Java stack trace:");
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.out.println("Runtime error: " + e.getMessage());
            e.printStackTrace();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // process system event queue
        virtualBoxManager.waitForEvents(0);
        if (ws) {
            try {
                virtualBoxManager.disconnect();
            } catch (VBoxException e) {
                e.printStackTrace();
            }
        }

        virtualBoxManager.cleanup();
        System.out.println("finished");
    }

}
