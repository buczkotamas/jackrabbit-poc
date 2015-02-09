package gui;

import com.naviextras.zippy.apis.services.market.FingerprintRO;
import com.naviextras.zippy.apis.services.market.ProcessRO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement public class Wrapper
{
    private FingerprintRO fingerprint;
    private ProcessRO process;

    public Wrapper()
    {
    }

    public Wrapper(FingerprintRO fingerprint)
    {
        this.fingerprint = fingerprint;
    }

    public Wrapper(ProcessRO process)
    {
        this.process = process;
    }

    public FingerprintRO getFingerprint()
    {
        return fingerprint;
    }

    public void setFingerprint(FingerprintRO fingerprint)
    {
        this.fingerprint = fingerprint;
    }

    public ProcessRO getProcess()
    {
        return process;
    }

    public void setProcess(ProcessRO process)
    {
        this.process = process;
    }
}
