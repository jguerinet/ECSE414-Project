/**
 * Created with IntelliJ IDEA.
 * User: Julien
 * Date: 19/11/13
 * Time: 2:14 AM
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Peer {
    private String name;
    private String externalAddress;
    private String externalPort;
    private String internalAddress;
    private String internalPort;
    @JsonProperty("peer_id")
    private String peerId;

    public Peer(){}

    public Peer(String name, String internalAddress, String internalPort,
                String externalAddress, String externalPort){
        this.name = name;
        this.internalAddress = internalAddress;
        this.internalPort = internalPort;
        this.externalAddress = externalAddress;
        this.externalPort = externalPort;
    }

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

    public String getId(){
        return this.peerId;
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

    public void setId(String id){
        this.peerId = id;
    }
}
