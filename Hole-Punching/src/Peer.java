/**
 * Created with IntelliJ IDEA.
 * User: Julien
 * Date: 19/11/13
 * Time: 2:14 AM
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Peer {
    private String name;
    private String externalAddress;
    private String externalPort;
    private String internalAddress;
    private String internalPort;

    public String getName(){
        return this.name;
    }

    public String getExternalAddress(){
        return this.externalAddress;
    }

    public String getExternalPort(){
        return this.externalPort;
    }

    public String getInternalAddress(){
        return this.internalAddress;
    }

    public String getInternalPort(){
        return this.internalPort;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setExternalAddress(String externalAddress){
        this.externalAddress = externalAddress;
    }

    public void setExternalPort(String externalPort){
        this.externalPort = externalPort;
    }

    public void setInternalAddress(String internalAddress){
        this.internalAddress = internalAddress;
    }

    public void setInternalPort(String internalPort){
        this.internalPort = internalPort;
    }
}
