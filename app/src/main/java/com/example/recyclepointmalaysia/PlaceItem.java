package com.example.recyclepointmalaysia;

import java.io.Serializable;

public class PlaceItem implements Serializable {
    public String name;
    public String address;
    public String phone;
    public String coordinates;
    public double rating;
    public String photoUrl;
    public String placeId;

 
    public PlaceItem() {
    }

    // Constructor for API data
    public PlaceItem(String name, String address, String phone,
                     String coordinates, double rating, String photoUrl) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.coordinates = coordinates;
        this.rating = rating;
        this.photoUrl = photoUrl;
    }

    // Constructor for Firebase data
    public PlaceItem(String name, String address, String phone,
                     String coordinates, double rating, String photoUrl, String placeId) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.coordinates = coordinates;
        this.rating = rating;
        this.photoUrl = photoUrl;
        this.placeId = placeId;
    }

    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
